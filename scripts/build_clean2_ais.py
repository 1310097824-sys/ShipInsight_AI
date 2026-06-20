from __future__ import annotations

import argparse
import csv
import re
import sys
from datetime import datetime
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
LOCAL_DEPS = ROOT / ".codex_deps"
if LOCAL_DEPS.exists():
    sys.path.insert(0, str(LOCAL_DEPS))

try:
    import zstandard as zstd
except ImportError as exc:  # pragma: no cover - environment guidance
    raise SystemExit(
        "Missing zstandard. Run this script with the bundled Python 3.12 runtime "
        "or install zstandard for the active Python interpreter."
    ) from exc

import pandas as pd


DATA_DIR = ROOT / "data"
SELECTED_SHIPS_PATH = ROOT / "clean1" / "selected_ships.csv"
OUTPUT_PATH = ROOT / "clean2" / "clean2_ais.csv"

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
    *INPUT_COLUMNS,
    "source_file",
]

STRING_COLUMNS = {"vessel_name", "imo", "call_sign", "transceiver"}
FLOAT_RULES = {
    "longitude": (-180.0, 180.0, 6),
    "latitude": (-90.0, 90.0, 6),
    "sog": (0.0, 102.2, 1),
    "cog": (0.0, 359.9999, 1),
    "length": (0.0, 500.0, 1),
    "width": (0.0, 100.0, 1),
    "draft": (0.0, 30.0, 1),
}
INTEGER_RULES = {
    "heading": (0, 359),
    "vessel_type": (0, 65535),
    "status": (0, 65535),
    "cargo": (0, 65535),
}
START_DATE = datetime(2025, 3, 1)
END_DATE = datetime(2025, 4, 30, 23, 59, 59)
CHUNKSIZE = 500_000


def iter_input_files(data_dir: Path) -> Iterable[Path]:
    seen: set[Path] = set()
    for pattern in ("ais-2025-03-*", "ais-2025-04-*"):
        for path in sorted(data_dir.glob(pattern)):
            if path.is_file() and path not in seen:
                seen.add(path)
                yield path


def load_selected_mmsi(path: Path, limit: int) -> set[str]:
    if not path.exists():
        raise SystemExit(f"Selected ship file not found: {path}")

    selected: list[str] = []
    with path.open("r", encoding="utf-8", newline="") as handle:
        for row in csv.DictReader(handle):
            mmsi = normalize_mmsi(row.get("mmsi", ""))
            if mmsi and mmsi not in selected:
                selected.append(mmsi)
            if len(selected) >= limit:
                break

    if len(selected) != limit:
        raise SystemExit(f"Expected {limit} selected ships, found {len(selected)} in {path}")
    return set(selected)


def normalize_mmsi(value: str | None) -> str:
    text = (value or "").strip()
    return text if re.fullmatch(r"\d{9}", text) else ""


def normalize_text(value: str | None) -> str:
    return re.sub(r"\s+", " ", (value or "").strip()).upper()


def normalize_datetime(value: str | None) -> str:
    text = (value or "").strip()[:19]
    try:
        parsed = datetime.strptime(text, "%Y-%m-%d %H:%M:%S")
    except ValueError:
        return ""
    if parsed < START_DATE or parsed > END_DATE:
        return ""
    return parsed.strftime("%Y-%m-%d %H:%M:%S")


def parse_float(value: str | None, minimum: float, maximum: float, digits: int) -> str:
    text = (value or "").strip()
    if not text:
        return ""
    try:
        parsed = float(text)
    except ValueError:
        return ""
    if parsed < minimum or parsed > maximum:
        return ""
    rendered = f"{parsed:.{digits}f}".rstrip("0").rstrip(".")
    return rendered


def parse_int(value: str | None, minimum: int, maximum: int) -> str:
    text = (value or "").strip()
    if not text:
        return ""
    try:
        parsed_float = float(text)
    except ValueError:
        return ""
    if not parsed_float.is_integer():
        return ""
    parsed = int(parsed_float)
    if parsed < minimum or parsed > maximum:
        return ""
    return str(parsed)


def normalize_text_series(series: pd.Series) -> pd.Series:
    return (
        series.astype("string")
        .fillna("")
        .str.strip()
        .str.replace(r"\s+", " ", regex=True)
        .str.upper()
    )


def trim_float_series(series: pd.Series, digits: int) -> pd.Series:
    return series.map(lambda value: "" if pd.isna(value) else f"{value:.{digits}f}".rstrip("0").rstrip(".")).astype("string")


def trim_int_series(series: pd.Series) -> pd.Series:
    return series.map(lambda value: "" if pd.isna(value) else str(int(value))).astype("string")


def clean_chunk(chunk: pd.DataFrame, selected_mmsi: set[str], source_file: str) -> pd.DataFrame:
    if list(chunk.columns) != INPUT_COLUMNS:
        raise ValueError(f"{source_file} has unexpected columns: {list(chunk.columns)}")

    chunk = chunk.copy()
    chunk["mmsi"] = chunk["mmsi"].astype("string").str.strip()
    chunk = chunk[chunk["mmsi"].isin(selected_mmsi)].copy()
    if chunk.empty:
        return pd.DataFrame(columns=OUTPUT_COLUMNS)

    chunk["base_date_time"] = pd.to_datetime(
        chunk["base_date_time"].astype("string").str.slice(0, 19),
        errors="coerce",
        format="%Y-%m-%d %H:%M:%S",
    )
    chunk["longitude"] = pd.to_numeric(chunk["longitude"], errors="coerce")
    chunk["latitude"] = pd.to_numeric(chunk["latitude"], errors="coerce")
    chunk = chunk[
        chunk["base_date_time"].between(START_DATE, END_DATE, inclusive="both")
        & chunk["longitude"].between(*FLOAT_RULES["longitude"][:2], inclusive="both")
        & chunk["latitude"].between(*FLOAT_RULES["latitude"][:2], inclusive="both")
    ].copy()
    if chunk.empty:
        return pd.DataFrame(columns=OUTPUT_COLUMNS)

    for column, (minimum, maximum, _digits) in FLOAT_RULES.items():
        if column in ("longitude", "latitude"):
            continue
        chunk[column] = pd.to_numeric(chunk[column], errors="coerce")
        chunk.loc[~chunk[column].between(minimum, maximum, inclusive="both"), column] = pd.NA

    for column, (minimum, maximum) in INTEGER_RULES.items():
        chunk[column] = pd.to_numeric(chunk[column], errors="coerce")
        chunk.loc[~chunk[column].between(minimum, maximum, inclusive="both"), column] = pd.NA
        chunk.loc[chunk[column].notna() & (chunk[column] % 1 != 0), column] = pd.NA

    cleaned = pd.DataFrame(
        {
            "mmsi": chunk["mmsi"].astype("string"),
            "base_date_time": chunk["base_date_time"].dt.strftime("%Y-%m-%d %H:%M:%S").astype("string"),
            "longitude": trim_float_series(chunk["longitude"], FLOAT_RULES["longitude"][2]),
            "latitude": trim_float_series(chunk["latitude"], FLOAT_RULES["latitude"][2]),
            "sog": trim_float_series(chunk["sog"], FLOAT_RULES["sog"][2]),
            "cog": trim_float_series(chunk["cog"], FLOAT_RULES["cog"][2]),
            "heading": trim_int_series(chunk["heading"]),
            "vessel_name": normalize_text_series(chunk["vessel_name"]),
            "imo": normalize_text_series(chunk["imo"]),
            "call_sign": normalize_text_series(chunk["call_sign"]),
            "vessel_type": trim_int_series(chunk["vessel_type"]),
            "status": trim_int_series(chunk["status"]),
            "length": trim_float_series(chunk["length"], FLOAT_RULES["length"][2]),
            "width": trim_float_series(chunk["width"], FLOAT_RULES["width"][2]),
            "draft": trim_float_series(chunk["draft"], FLOAT_RULES["draft"][2]),
            "cargo": trim_int_series(chunk["cargo"]),
            "transceiver": normalize_text_series(chunk["transceiver"]),
            "source_file": source_file,
        }
    )
    return cleaned[OUTPUT_COLUMNS]


def process_file(
    path: Path,
    output_path: Path,
    selected_mmsi: set[str],
    seen_keys: set[tuple[str, str, str, str]],
    write_header: bool,
) -> tuple[int, int, int]:
    read_rows = 0
    kept_rows = 0
    duplicate_rows = 0
    for chunk in pd.read_csv(
        path,
        compression="zstd",
        chunksize=CHUNKSIZE,
        dtype="string",
        keep_default_na=False,
        na_filter=False,
    ):
        read_rows += len(chunk)
        cleaned = clean_chunk(chunk, selected_mmsi, path.name)
        if cleaned.empty:
            continue

        key_columns = ["mmsi", "base_date_time", "longitude", "latitude"]
        in_chunk_duplicate_mask = cleaned.duplicated(subset=key_columns, keep="first")
        duplicate_rows += int(in_chunk_duplicate_mask.sum())
        if in_chunk_duplicate_mask.any():
            cleaned = cleaned.loc[~in_chunk_duplicate_mask].copy()

        keys = list(zip(cleaned["mmsi"], cleaned["base_date_time"], cleaned["longitude"], cleaned["latitude"]))
        existing_duplicate_mask = [key in seen_keys for key in keys]
        duplicate_rows += sum(existing_duplicate_mask)
        if any(existing_duplicate_mask):
            cleaned = cleaned.loc[[not value for value in existing_duplicate_mask]].copy()
            keys = [key for key, is_duplicate in zip(keys, existing_duplicate_mask) if not is_duplicate]

        seen_keys.update(keys)
        kept_rows += len(cleaned)
        cleaned.to_csv(
            output_path,
            mode="a",
            header=write_header,
            index=False,
            encoding="utf-8",
            lineterminator="\n",
        )
        write_header = False
    return read_rows, kept_rows, duplicate_rows


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Build clean2 AIS records for 100 display-friendly ships in March and April 2025."
    )
    parser.add_argument("--data-dir", type=Path, default=DATA_DIR)
    parser.add_argument("--selected-ships", type=Path, default=SELECTED_SHIPS_PATH)
    parser.add_argument("--output", type=Path, default=OUTPUT_PATH)
    parser.add_argument("--ship-count", type=int, default=100)
    parser.add_argument("--limit-files", type=int, default=0)
    args = parser.parse_args()

    input_files = list(iter_input_files(args.data_dir))
    if args.limit_files:
        input_files = input_files[: args.limit_files]
    if len(input_files) != (args.limit_files or 61):
        raise SystemExit(f"Expected 61 March/April AIS files, found {len(input_files)} in {args.data_dir}")

    selected_mmsi = load_selected_mmsi(args.selected_ships, args.ship_count)
    args.output.parent.mkdir(parents=True, exist_ok=True)

    seen_keys: set[tuple[str, str, str, str]] = set()
    total_read = 0
    total_kept = 0
    total_duplicates = 0
    if args.output.exists():
        args.output.unlink()

    write_header = True
    for index, path in enumerate(input_files, start=1):
        read_rows, kept_rows, duplicate_rows = process_file(path, args.output, selected_mmsi, seen_keys, write_header)
        write_header = False
        total_read += read_rows
        total_kept += kept_rows
        total_duplicates += duplicate_rows
        print(
            f"[{index:02d}/{len(input_files):02d}] {path.name}: "
            f"read={read_rows:,} kept={kept_rows:,} duplicates={duplicate_rows:,}",
            flush=True,
        )

    print(f"output={args.output}")
    print(f"selected_ships={len(selected_mmsi):,}")
    print(f"files={len(input_files):,}")
    print(f"read_rows={total_read:,}")
    print(f"kept_rows={total_kept:,}")
    print(f"duplicate_rows={total_duplicates:,}")


if __name__ == "__main__":
    main()
