from __future__ import annotations

import argparse
import csv
import math
import sys
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
LOCAL_DEPS = ROOT / ".codex_deps"
if LOCAL_DEPS.exists():
    sys.path.insert(0, str(LOCAL_DEPS))

try:
    import zstandard as zstd  # noqa: F401 - used by pandas zstd compression
except ImportError as exc:  # pragma: no cover - environment guidance
    raise SystemExit(
        "Missing zstandard. Run this script with the bundled Python 3.12 runtime "
        "or install zstandard for the active Python interpreter."
    ) from exc

import pandas as pd


DATA_DIR = ROOT / "data"
OUTPUT_PATH = ROOT / "clean2" / "clean3_ais.csv"
SELECTED_SHIPS_PATH = ROOT / "clean2" / "clean3_selected_ships.csv"
CANDIDATE_SCORES_PATH = ROOT / "clean2" / "clean3_candidate_scores.csv"

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
OUTPUT_COLUMNS = [*INPUT_COLUMNS, "source_file"]
SELECTION_COLUMNS = [
    "mmsi",
    "base_date_time",
    "longitude",
    "latitude",
    "sog",
    "vessel_name",
    "vessel_type",
    "length",
    "width",
]
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
STRING_COLUMNS = {"vessel_name", "imo", "call_sign", "transceiver"}
CHUNKSIZE = 750_000


@dataclass
class Candidate:
    mmsi: str
    latest_time: str
    latest_lon: float
    latest_lat: float
    vessel_name: str
    vessel_type: str
    length: str
    width: str
    records_on_day: int
    moving_ratio: float
    lon_span: float
    lat_span: float
    score: float
    cell: str


def iter_input_files(data_dir: Path) -> Iterable[Path]:
    seen: set[Path] = set()
    for pattern in ("ais-2025-03-*", "ais-2025-04-*"):
        for path in sorted(data_dir.glob(pattern)):
            if path.is_file() and path not in seen:
                seen.add(path)
                yield path


def latest_day_file(data_dir: Path) -> Path:
    path = data_dir / "ais-2025-04-30.csv.zst"
    if not path.exists():
        raise SystemExit(f"Selection source file not found: {path}")
    return path


def text_series(series: pd.Series) -> pd.Series:
    return series.astype(str).fillna("").str.strip()


def normalize_name(series: pd.Series) -> pd.Series:
    return text_series(series).str.replace(r"\s+", " ", regex=True).str.upper()


def trim_float_series(series: pd.Series, digits: int) -> pd.Series:
    return series.map(lambda value: "" if pd.isna(value) else f"{value:.{digits}f}".rstrip("0").rstrip(".")).astype(str)


def trim_int_series(series: pd.Series) -> pd.Series:
    return series.map(lambda value: "" if pd.isna(value) else str(int(value))).astype(str)


def haversine_km(lon1: float, lat1: float, lon2: float, lat2: float) -> float:
    radius = 6371.0088
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlambda = math.radians(lon2 - lon1)
    a = math.sin(dphi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(dlambda / 2) ** 2
    return 2 * radius * math.asin(math.sqrt(a))


def grid_cell(lon: float, lat: float, cell_degrees: float) -> str:
    lon_cell = math.floor((lon + 180.0) / cell_degrees)
    lat_cell = math.floor((lat + 90.0) / cell_degrees)
    return f"{lat_cell}:{lon_cell}"


def update_candidate_stats(stats: dict[str, dict[str, object]], chunk: pd.DataFrame) -> None:
    chunk = chunk.copy()
    chunk["mmsi"] = text_series(chunk["mmsi"])
    chunk["base_date_time"] = pd.to_datetime(
        text_series(chunk["base_date_time"]).str.slice(0, 19),
        errors="coerce",
        format="%Y-%m-%d %H:%M:%S",
    )
    chunk["longitude"] = pd.to_numeric(chunk["longitude"], errors="coerce")
    chunk["latitude"] = pd.to_numeric(chunk["latitude"], errors="coerce")
    chunk["sog"] = pd.to_numeric(chunk["sog"], errors="coerce")
    chunk["vessel_name"] = normalize_name(chunk["vessel_name"])
    chunk = chunk[
        chunk["mmsi"].str.fullmatch(r"\d{9}", na=False)
        & chunk["base_date_time"].notna()
        & chunk["longitude"].between(-180, 180, inclusive="both")
        & chunk["latitude"].between(-90, 90, inclusive="both")
        & ~((chunk["longitude"].abs() < 0.000001) & (chunk["latitude"].abs() < 0.000001))
        & (chunk["vessel_name"] != "")
    ].copy()
    if chunk.empty:
        return

    chunk = chunk.sort_values("base_date_time").drop_duplicates("mmsi", keep="last")
    for row in chunk.itertuples(index=False):
        mmsi = row.mmsi
        current = stats.get(mmsi)
        if current is not None and row.base_date_time <= current["latest_time"]:
            continue
        stats[mmsi] = {
            "mmsi": mmsi,
            "records_on_day": 1,
            "moving_count": 1 if float(row.sog) > 0.5 else 0,
            "named_count": 1,
            "lon_min": float(row.longitude),
            "lon_max": float(row.longitude),
            "lat_min": float(row.latitude),
            "lat_max": float(row.latitude),
            "latest_time": row.base_date_time,
            "latest_lon": float(row.longitude),
            "latest_lat": float(row.latitude),
            "vessel_name": str(row.vessel_name),
            "vessel_type": str(row.vessel_type).strip(),
            "length": str(row.length).strip(),
            "width": str(row.width).strip(),
        }


def build_candidates(selection_file: Path, cell_degrees: float) -> list[Candidate]:
    stats: dict[str, dict[str, object]] = {}
    for index, chunk in enumerate(pd.read_csv(
        selection_file,
        compression="zstd",
        usecols=SELECTION_COLUMNS,
        chunksize=CHUNKSIZE,
        dtype=str,
        keep_default_na=False,
        na_filter=False,
    ), start=1):
        update_candidate_stats(stats, chunk)
        if index % 5 == 0:
            print(f"selection_chunks={index:,} latest_positions={len(stats):,}", flush=True)

    candidates: list[Candidate] = []
    for values in stats.values():
        records = int(values["records_on_day"])
        named_ratio = int(values["named_count"]) / max(records, 1)
        if not str(values["vessel_name"]).strip():
            continue

        lon = float(values["latest_lon"])
        lat = float(values["latest_lat"])
        lon_span = float(values["lon_max"]) - float(values["lon_min"])
        lat_span = float(values["lat_max"]) - float(values["lat_min"])
        moving_ratio = int(values["moving_count"]) / max(records, 1)
        abs_lat_penalty = max(0, abs(lat) - 70) * 3
        score = 100 + moving_ratio * 20 + named_ratio * 10 - abs_lat_penalty
        candidates.append(
            Candidate(
                mmsi=str(values["mmsi"]),
                latest_time=values["latest_time"].strftime("%Y-%m-%d %H:%M:%S"),
                latest_lon=lon,
                latest_lat=lat,
                vessel_name=str(values["vessel_name"]),
                vessel_type=str(values["vessel_type"]),
                length=str(values["length"]),
                width=str(values["width"]),
                records_on_day=records,
                moving_ratio=moving_ratio,
                lon_span=lon_span,
                lat_span=lat_span,
                score=score,
                cell=grid_cell(lon, lat, cell_degrees),
            )
        )
    return candidates


def select_spread(candidates: list[Candidate], ship_count: int, per_cell_keep: int) -> list[Candidate]:
    if len(candidates) < ship_count:
        raise SystemExit(f"Only {len(candidates)} usable candidates found, need {ship_count}")

    by_cell: dict[str, list[Candidate]] = {}
    for candidate in sorted(candidates, key=lambda item: item.score, reverse=True):
        bucket = by_cell.setdefault(candidate.cell, [])
        if len(bucket) < per_cell_keep:
            bucket.append(candidate)
    pool = [candidate for bucket in by_cell.values() for candidate in bucket]
    if len(pool) < ship_count:
        pool = sorted(candidates, key=lambda item: item.score, reverse=True)[: max(ship_count * 8, ship_count)]

    selected = [max(pool, key=lambda item: item.score)]
    remaining = [candidate for candidate in pool if candidate.mmsi != selected[0].mmsi]
    max_score = max(candidate.score for candidate in pool) or 1.0

    while len(selected) < ship_count and remaining:
        best_index = 0
        best_value = -1.0
        for index, candidate in enumerate(remaining):
            min_distance = min(
                haversine_km(candidate.latest_lon, candidate.latest_lat, chosen.latest_lon, chosen.latest_lat)
                for chosen in selected
            )
            value = min_distance + 80 * (candidate.score / max_score)
            if value > best_value:
                best_value = value
                best_index = index
        selected.append(remaining.pop(best_index))

    if len(selected) != ship_count:
        raise SystemExit(f"Selected {len(selected)} ships, expected {ship_count}")
    return selected


def write_candidates(path: Path, candidates: list[Candidate]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    rows = [candidate.__dict__ for candidate in sorted(candidates, key=lambda item: item.score, reverse=True)]
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0].keys()), lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def clean_chunk(chunk: pd.DataFrame, selected_mmsi: set[str], source_file: str) -> pd.DataFrame:
    if list(chunk.columns) != INPUT_COLUMNS:
        raise ValueError(f"{source_file} has unexpected columns: {list(chunk.columns)}")

    chunk = chunk.copy()
    chunk["mmsi"] = text_series(chunk["mmsi"])
    chunk = chunk[chunk["mmsi"].isin(selected_mmsi)].copy()
    if chunk.empty:
        return pd.DataFrame(columns=OUTPUT_COLUMNS)

    chunk["base_date_time"] = pd.to_datetime(
        text_series(chunk["base_date_time"]).str.slice(0, 19),
        errors="coerce",
        format="%Y-%m-%d %H:%M:%S",
    )
    chunk["longitude"] = pd.to_numeric(chunk["longitude"], errors="coerce")
    chunk["latitude"] = pd.to_numeric(chunk["latitude"], errors="coerce")
    chunk = chunk[
        chunk["base_date_time"].between(START_DATE, END_DATE, inclusive="both")
        & chunk["longitude"].between(*FLOAT_RULES["longitude"][:2], inclusive="both")
        & chunk["latitude"].between(*FLOAT_RULES["latitude"][:2], inclusive="both")
        & ~((chunk["longitude"].abs() < 0.000001) & (chunk["latitude"].abs() < 0.000001))
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
            "mmsi": chunk["mmsi"].astype(str),
            "base_date_time": chunk["base_date_time"].dt.strftime("%Y-%m-%d %H:%M:%S").astype(str),
            "longitude": trim_float_series(chunk["longitude"], FLOAT_RULES["longitude"][2]),
            "latitude": trim_float_series(chunk["latitude"], FLOAT_RULES["latitude"][2]),
            "sog": trim_float_series(chunk["sog"], FLOAT_RULES["sog"][2]),
            "cog": trim_float_series(chunk["cog"], FLOAT_RULES["cog"][2]),
            "heading": trim_int_series(chunk["heading"]),
            "vessel_name": normalize_name(chunk["vessel_name"]),
            "imo": normalize_name(chunk["imo"]),
            "call_sign": normalize_name(chunk["call_sign"]),
            "vessel_type": trim_int_series(chunk["vessel_type"]),
            "status": trim_int_series(chunk["status"]),
            "length": trim_float_series(chunk["length"], FLOAT_RULES["length"][2]),
            "width": trim_float_series(chunk["width"], FLOAT_RULES["width"][2]),
            "draft": trim_float_series(chunk["draft"], FLOAT_RULES["draft"][2]),
            "cargo": trim_int_series(chunk["cargo"]),
            "transceiver": normalize_name(chunk["transceiver"]),
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
        dtype=str,
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
    parser = argparse.ArgumentParser(description="Build spatially spread AIS records for map display.")
    parser.add_argument("--data-dir", type=Path, default=DATA_DIR)
    parser.add_argument("--output", type=Path, default=OUTPUT_PATH)
    parser.add_argument("--selected-ships-output", type=Path, default=SELECTED_SHIPS_PATH)
    parser.add_argument("--candidate-scores-output", type=Path, default=CANDIDATE_SCORES_PATH)
    parser.add_argument("--ship-count", type=int, default=100)
    parser.add_argument("--cell-degrees", type=float, default=2.0)
    parser.add_argument("--per-cell-keep", type=int, default=4)
    parser.add_argument("--limit-files", type=int, default=0)
    parser.add_argument("--select-only", action="store_true")
    args = parser.parse_args()

    input_files = list(iter_input_files(args.data_dir))
    if args.limit_files:
        input_files = input_files[: args.limit_files]
    if len(input_files) != (args.limit_files or 61):
        raise SystemExit(f"Expected 61 March/April AIS files, found {len(input_files)} in {args.data_dir}")

    print(f"selection_source={latest_day_file(args.data_dir)}", flush=True)
    candidates = build_candidates(latest_day_file(args.data_dir), args.cell_degrees)
    selected = select_spread(candidates, args.ship_count, args.per_cell_keep)
    write_candidates(args.candidate_scores_output, candidates)
    write_candidates(args.selected_ships_output, selected)
    print(
        f"candidates={len(candidates):,} selected={len(selected):,} "
        f"selected_cells={len({candidate.cell for candidate in selected}):,} "
        f"lon_range=[{min(c.latest_lon for c in selected):.3f},{max(c.latest_lon for c in selected):.3f}] "
        f"lat_range=[{min(c.latest_lat for c in selected):.3f},{max(c.latest_lat for c in selected):.3f}]",
        flush=True,
    )

    if args.select_only:
        return

    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.exists():
        args.output.unlink()

    selected_mmsi = {candidate.mmsi for candidate in selected}
    seen_keys: set[tuple[str, str, str, str]] = set()
    total_read = 0
    total_kept = 0
    total_duplicates = 0
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

    print(f"output={args.output}", flush=True)
    print(f"selected_ships={len(selected_mmsi):,}", flush=True)
    print(f"files={len(input_files):,}", flush=True)
    print(f"read_rows={total_read:,}", flush=True)
    print(f"kept_rows={total_kept:,}", flush=True)
    print(f"duplicate_rows={total_duplicates:,}", flush=True)


if __name__ == "__main__":
    main()
