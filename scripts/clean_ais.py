from __future__ import annotations

import argparse
import sys
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
LOCAL_DEPS = ROOT / ".codex_deps"
if LOCAL_DEPS.exists():
    sys.path.insert(0, str(LOCAL_DEPS))

import pandas as pd


DATA_DIR = ROOT / "data"
CLEAN_DIR = ROOT / "clean"
OUTPUT_PATH = CLEAN_DIR / "clean_ais.csv"

# San Pedro Bay area covering Port of Los Angeles, Port of Long Beach,
# and nearby anchorage lanes.
MIN_LAT = 33.65
MAX_LAT = 33.85
MIN_LON = -118.35
MAX_LON = -118.05

INPUT_COLUMNS = [
    "mmsi",
    "base_date_time",
    "longitude",
    "latitude",
    "sog",
    "cog",
    "heading",
    "vessel_name",
    "imo",
    "call_sign",
    "vessel_type",
    "status",
    "length",
    "width",
    "draft",
    "cargo",
    "transceiver",
]

OUTPUT_COLUMNS = [
    "mmsi",
    "base_date_time",
    "longitude",
    "latitude",
    "sog",
    "cog",
    "heading",
    "vessel_name",
    "imo",
    "call_sign",
    "vessel_type",
    "status",
    "length",
    "width",
    "draft",
    "cargo",
    "transceiver",
    "source_file",
]

STRING_COLUMNS = ["vessel_name", "imo", "call_sign", "transceiver"]
INTEGER_COLUMNS = ["heading", "vessel_type", "status", "cargo"]
FLOAT_COLUMNS = ["longitude", "latitude", "sog", "cog", "length", "width", "draft"]


def iter_input_files(data_dir: Path) -> Iterable[Path]:
    patterns = ("ais-2025-03-*", "ais-2025-04-*")
    seen: set[Path] = set()
    for pattern in patterns:
        for path in sorted(data_dir.glob(pattern)):
            if path.is_file() and path not in seen:
                seen.add(path)
                yield path


def normalize_text(series: pd.Series) -> pd.Series:
    return (
        series.astype("string")
        .fillna("")
        .str.strip()
        .str.replace(r"\s+", " ", regex=True)
        .str.upper()
    )


def normalize_mmsi(series: pd.Series) -> pd.Series:
    return series.astype("string").str.strip()


def normalize_datetime(series: pd.Series) -> pd.Series:
    parsed = pd.to_datetime(series.astype("string").str.slice(0, 19), errors="coerce", format="%Y-%m-%d %H:%M:%S")
    return parsed.dt.strftime("%Y-%m-%d %H:%M:%S")


def numeric(series: pd.Series) -> pd.Series:
    return pd.to_numeric(series, errors="coerce")


def trim_float(series: pd.Series, digits: int) -> pd.Series:
    rounded = series.round(digits)
    text = rounded.map(lambda value: "" if pd.isna(value) else f"{value:.{digits}f}".rstrip("0").rstrip("."))
    return text.astype("string")


def trim_int(series: pd.Series) -> pd.Series:
    text = series.map(lambda value: "" if pd.isna(value) else str(int(value)))
    return text.astype("string")


def clean_chunk(chunk: pd.DataFrame, source_file: str) -> pd.DataFrame:
    if list(chunk.columns) != INPUT_COLUMNS:
        raise ValueError(f"{source_file} has unexpected columns: {list(chunk.columns)}")

    chunk = chunk.copy()
    chunk["longitude"] = numeric(chunk["longitude"])
    chunk["latitude"] = numeric(chunk["latitude"])
    mask = (
        chunk["longitude"].between(MIN_LON, MAX_LON, inclusive="both")
        & chunk["latitude"].between(MIN_LAT, MAX_LAT, inclusive="both")
    )
    chunk = chunk.loc[mask].copy()
    if chunk.empty:
        return pd.DataFrame(columns=OUTPUT_COLUMNS)

    chunk["mmsi"] = normalize_mmsi(chunk["mmsi"])
    chunk["base_date_time"] = normalize_datetime(chunk["base_date_time"])
    chunk = chunk[
        chunk["mmsi"].str.fullmatch(r"\d{9}", na=False)
        & chunk["base_date_time"].notna()
        & (chunk["base_date_time"] != "")
    ].copy()
    if chunk.empty:
        return pd.DataFrame(columns=OUTPUT_COLUMNS)

    for column in FLOAT_COLUMNS:
        if column not in ("longitude", "latitude"):
            chunk[column] = numeric(chunk[column])

    chunk.loc[~chunk["sog"].between(0, 102.2, inclusive="both"), "sog"] = pd.NA
    chunk.loc[~chunk["cog"].between(0, 359.9999, inclusive="both"), "cog"] = pd.NA
    chunk.loc[~chunk["length"].between(0, 500, inclusive="both"), "length"] = pd.NA
    chunk.loc[~chunk["width"].between(0, 100, inclusive="both"), "width"] = pd.NA
    chunk.loc[~chunk["draft"].between(0, 30, inclusive="both"), "draft"] = pd.NA

    for column in INTEGER_COLUMNS:
        chunk[column] = numeric(chunk[column])

    chunk.loc[~chunk["heading"].between(0, 359, inclusive="both"), "heading"] = pd.NA
    for column in INTEGER_COLUMNS:
        integer_values = chunk[column]
        chunk.loc[integer_values.notna() & (integer_values % 1 != 0), column] = pd.NA

    for column in STRING_COLUMNS:
        chunk[column] = normalize_text(chunk[column])

    cleaned = pd.DataFrame(
        {
            "mmsi": chunk["mmsi"].astype("string"),
            "base_date_time": chunk["base_date_time"].astype("string"),
            "longitude": trim_float(chunk["longitude"], 6),
            "latitude": trim_float(chunk["latitude"], 6),
            "sog": trim_float(chunk["sog"], 1),
            "cog": trim_float(chunk["cog"], 1),
            "heading": trim_int(chunk["heading"]),
            "vessel_name": chunk["vessel_name"],
            "imo": chunk["imo"],
            "call_sign": chunk["call_sign"],
            "vessel_type": trim_int(chunk["vessel_type"]),
            "status": trim_int(chunk["status"]),
            "length": trim_float(chunk["length"], 1),
            "width": trim_float(chunk["width"], 1),
            "draft": trim_float(chunk["draft"], 1),
            "cargo": trim_int(chunk["cargo"]),
            "transceiver": chunk["transceiver"],
            "source_file": source_file,
        }
    )
    return cleaned[OUTPUT_COLUMNS]


def process_file(path: Path, output_path: Path, seen_keys: set[tuple[str, str, str, str]], write_header: bool) -> tuple[int, int, int]:
    total = 0
    kept = 0
    duplicates = 0
    for chunk in pd.read_csv(
        path,
        compression="zstd",
        chunksize=500_000,
        dtype="string",
        keep_default_na=False,
        na_filter=False,
    ):
        total += len(chunk)
        cleaned = clean_chunk(chunk, path.name)
        if cleaned.empty:
            continue
        key_columns = ["mmsi", "base_date_time", "longitude", "latitude"]
        in_chunk_duplicate_mask = cleaned.duplicated(subset=key_columns, keep="first")
        duplicates += int(in_chunk_duplicate_mask.sum())
        if in_chunk_duplicate_mask.any():
            cleaned = cleaned.loc[~in_chunk_duplicate_mask].copy()
        keys = list(zip(cleaned["mmsi"], cleaned["base_date_time"], cleaned["longitude"], cleaned["latitude"]))
        duplicate_mask = [key in seen_keys for key in keys]
        duplicates += sum(duplicate_mask)
        if any(duplicate_mask):
            cleaned = cleaned.loc[[not value for value in duplicate_mask]].copy()
            keys = [key for key, is_duplicate in zip(keys, duplicate_mask) if not is_duplicate]
        seen_keys.update(keys)
        kept += len(cleaned)
        cleaned.to_csv(output_path, mode="a", header=write_header, index=False, encoding="utf-8", lineterminator="\n")
        write_header = False
    return total, kept, duplicates


def main() -> None:
    parser = argparse.ArgumentParser(description="Clean March/April 2025 AIS data for LA/LB port area.")
    parser.add_argument("--data-dir", type=Path, default=DATA_DIR)
    parser.add_argument("--output", type=Path, default=OUTPUT_PATH)
    parser.add_argument("--limit-files", type=int, default=0)
    args = parser.parse_args()

    input_files = list(iter_input_files(args.data_dir))
    if args.limit_files:
        input_files = input_files[: args.limit_files]
    if not input_files:
        raise SystemExit(f"No AIS input files found under {args.data_dir}")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.exists():
        args.output.unlink()

    seen_keys: set[tuple[str, str, str, str]] = set()
    total_rows = 0
    kept_rows = 0
    duplicate_rows = 0
    write_header = True

    for index, path in enumerate(input_files, start=1):
        total, kept, duplicates = process_file(path, args.output, seen_keys, write_header)
        write_header = False
        total_rows += total
        kept_rows += kept
        duplicate_rows += duplicates
        print(
            f"[{index:02d}/{len(input_files):02d}] {path.name}: "
            f"read={total:,} kept={kept:,} duplicates={duplicates:,}",
            flush=True,
        )

    print(f"output={args.output}")
    print(f"files={len(input_files):,}")
    print(f"read_rows={total_rows:,}")
    print(f"kept_rows={kept_rows:,}")
    print(f"duplicate_rows={duplicate_rows:,}")
    print(f"bounds=lat[{MIN_LAT},{MAX_LAT}] lon[{MIN_LON},{MAX_LON}]")


if __name__ == "__main__":
    main()
