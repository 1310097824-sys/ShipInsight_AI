from __future__ import annotations

import argparse
import csv
import re
import sys
import xml.etree.ElementTree as ET
from datetime import datetime, timedelta, timezone
from pathlib import Path

try:
    from zoneinfo import ZoneInfo, ZoneInfoNotFoundError
except ImportError:  # pragma: no cover - zoneinfo ships with modern Python
    ZoneInfo = None
    ZoneInfoNotFoundError = Exception


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT_DIR = ROOT / "clean"
DEFAULT_MMSI = "413000001"
DEFAULT_TIMEZONE = "Asia/Shanghai"
GPXTPX_NAMESPACE = "http://www.garmin.com/xmlschemas/TrackPointExtension/v1"
MS_TO_KNOTS = 1.9438444924406046
FIXED_TIMEZONES = {
    "UTC": timezone.utc,
    "Etc/UTC": timezone.utc,
    "Asia/Shanghai": timezone(timedelta(hours=8)),
    "Asia/Chongqing": timezone(timedelta(hours=8)),
    "Asia/Harbin": timezone(timedelta(hours=8)),
    "Asia/Urumqi": timezone(timedelta(hours=6)),
}

OUTPUT_COLUMNS = [
    "MMSI",
    "BaseDateTime",
    "LAT",
    "LON",
    "SOG",
    "COG",
    "Heading",
    "VesselName",
    "IMO",
    "CallSign",
    "VesselType",
    "Status",
    "Length",
    "Width",
    "Draft",
    "Cargo",
    "Transceiver",
    "SourceFile",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert a GPX track file into an AIS-like CSV that ShipInsight AI can import."
    )
    parser.add_argument("input_file", type=Path, help="Path to the source GPX file.")
    parser.add_argument(
        "-o",
        "--output-file",
        type=Path,
        help="Path to the generated CSV file. Defaults to clean/<input_stem>_ais.csv",
    )
    parser.add_argument(
        "--mmsi",
        default=DEFAULT_MMSI,
        help=f"MMSI written to every generated row. Defaults to {DEFAULT_MMSI}.",
    )
    parser.add_argument(
        "--vessel-name",
        help="Optional vessel name. Defaults to the input file stem.",
    )
    parser.add_argument(
        "--imo",
        default="",
        help="Optional IMO value to write into the CSV.",
    )
    parser.add_argument(
        "--call-sign",
        default="",
        help="Optional call sign to write into the CSV.",
    )
    parser.add_argument(
        "--transceiver",
        default="GPSLogger-Mobile",
        help="Optional transceiver/source label to write into the CSV.",
    )
    parser.add_argument(
        "--timezone",
        default=DEFAULT_TIMEZONE,
        help=f"Timezone used for exported timestamps. Defaults to {DEFAULT_TIMEZONE}.",
    )
    parser.add_argument(
        "--include-non-gps",
        action="store_true",
        help="Include points whose <src> is not gps, such as network points.",
    )
    return parser.parse_args()


def normalize_gpx_xml(text: str) -> str:
    if "gpxtpx:" in text and "xmlns:gpxtpx" not in text:
        return re.sub(
            r"<gpx\b",
            f'<gpx xmlns:gpxtpx="{GPXTPX_NAMESPACE}"',
            text,
            count=1,
        )
    return text


def local_name(tag: str) -> str:
    if "}" in tag:
        return tag.rsplit("}", 1)[-1]
    if ":" in tag:
        return tag.rsplit(":", 1)[-1]
    return tag


def child_text(element: ET.Element, name: str) -> str | None:
    for child in element:
        if local_name(child.tag).lower() == name.lower():
            text = (child.text or "").strip()
            return text or None
    return None


def descendant_float(element: ET.Element, name: str) -> float | None:
    for child in element.iter():
        if local_name(child.tag).lower() != name.lower():
            continue
        text = (child.text or "").strip()
        if not text:
            return None
        try:
            return float(text)
        except ValueError:
            return None
    return None


def parse_timestamp(value: str, timezone_name: str) -> datetime:
    target_zone = resolve_timezone(timezone_name)
    normalized = value.strip().replace("Z", "+00:00")
    parsed = datetime.fromisoformat(normalized)
    if parsed.tzinfo is None:
        return parsed
    return parsed.astimezone(target_zone).replace(tzinfo=None)


def resolve_timezone(name: str):
    normalized = name.strip()
    if not normalized:
        return timezone.utc
    if ZoneInfo is not None:
        try:
            return ZoneInfo(normalized)
        except ZoneInfoNotFoundError:
            pass
    if normalized in FIXED_TIMEZONES:
        return FIXED_TIMEZONES[normalized]

    offset_match = re.fullmatch(r"([+-])(\d{1,2})(?::?(\d{2}))?", normalized)
    if offset_match:
        sign = 1 if offset_match.group(1) == "+" else -1
        hours = int(offset_match.group(2))
        minutes = int(offset_match.group(3) or "0")
        return timezone(sign * timedelta(hours=hours, minutes=minutes))

    raise SystemExit(
        f"Unsupported timezone '{name}'. Try a fixed offset like +08:00 or a known name such as Asia/Shanghai."
    )


def format_decimal(value: float, digits: int) -> str:
    text = f"{value:.{digits}f}"
    return text.rstrip("0").rstrip(".") if "." in text else text


def heading_from_bearing(bearing: float | None) -> str:
    if bearing is None:
        return ""
    normalized = int(round(bearing)) % 360
    return str(normalized)


def iter_points(root: ET.Element, include_non_gps: bool, timezone_name: str):
    for point in root.iter():
        if local_name(point.tag).lower() != "trkpt":
            continue

        lat_text = point.attrib.get("lat")
        lon_text = point.attrib.get("lon")
        time_text = child_text(point, "time")
        src_text = (child_text(point, "src") or "").strip().lower()

        if not lat_text or not lon_text or not time_text:
            continue
        if not include_non_gps and src_text and src_text != "gps":
            continue

        try:
            lat = float(lat_text)
            lon = float(lon_text)
        except ValueError:
            continue

        try:
            observed_at = parse_timestamp(time_text, timezone_name)
        except ValueError:
            continue

        bearing = descendant_float(point, "bearing")
        speed_ms = descendant_float(point, "speed")
        speed_knots = speed_ms * MS_TO_KNOTS if speed_ms is not None else None

        yield {
            "observed_at": observed_at.strftime("%Y-%m-%d %H:%M:%S"),
            "lat": format_decimal(lat, 8),
            "lon": format_decimal(lon, 8),
            "sog": "" if speed_knots is None else format_decimal(speed_knots, 2),
            "cog": "" if bearing is None else format_decimal(bearing % 360, 1),
            "heading": heading_from_bearing(bearing),
        }


def convert_gpx_to_csv(
    input_file: Path,
    output_file: Path,
    mmsi: str,
    vessel_name: str,
    imo: str,
    call_sign: str,
    transceiver: str,
    timezone_name: str,
    include_non_gps: bool,
) -> int:
    xml_text = normalize_gpx_xml(input_file.read_text(encoding="utf-8"))
    root = ET.fromstring(xml_text)

    output_file.parent.mkdir(parents=True, exist_ok=True)
    rows_written = 0

    with output_file.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=OUTPUT_COLUMNS)
        writer.writeheader()

        for point in iter_points(root, include_non_gps=include_non_gps, timezone_name=timezone_name):
            writer.writerow(
                {
                    "MMSI": mmsi,
                    "BaseDateTime": point["observed_at"],
                    "LAT": point["lat"],
                    "LON": point["lon"],
                    "SOG": point["sog"],
                    "COG": point["cog"],
                    "Heading": point["heading"],
                    "VesselName": vessel_name,
                    "IMO": imo,
                    "CallSign": call_sign,
                    "VesselType": "",
                    "Status": "",
                    "Length": "",
                    "Width": "",
                    "Draft": "",
                    "Cargo": "",
                    "Transceiver": transceiver,
                    "SourceFile": input_file.name,
                }
            )
            rows_written += 1

    return rows_written


def main() -> int:
    args = parse_args()
    input_file = args.input_file.expanduser().resolve()
    if not input_file.is_file():
        raise SystemExit(f"GPX file not found: {input_file}")

    output_file = args.output_file
    if output_file is None:
        output_file = DEFAULT_OUTPUT_DIR / f"{input_file.stem}_ais.csv"
    output_file = output_file.expanduser().resolve()

    vessel_name = args.vessel_name or input_file.stem
    rows_written = convert_gpx_to_csv(
        input_file=input_file,
        output_file=output_file,
        mmsi=args.mmsi.strip(),
        vessel_name=vessel_name.strip(),
        imo=args.imo.strip(),
        call_sign=args.call_sign.strip(),
        transceiver=args.transceiver.strip(),
        timezone_name=args.timezone,
        include_non_gps=args.include_non_gps,
    )

    print(f"Generated {rows_written} AIS rows: {output_file}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
