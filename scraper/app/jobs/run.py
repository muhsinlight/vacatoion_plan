"""Elle çalıştırma: python -m app.jobs.run --job bus [--dry-run]"""

import argparse
import json
import logging
import sys

from app.jobs import JOBS
from app.settings import get_settings


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Vacation Plan scraper job'ları")
    parser.add_argument("--job", required=True, choices=[*JOBS, "all"])
    parser.add_argument("--dry-run", action="store_true", help="API'ye göndermeden logla")
    args = parser.parse_args(argv)

    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
    settings = get_settings()
    names = list(JOBS) if args.job == "all" else [args.job]

    failed = False
    for name in names:
        result = JOBS[name](settings, args.dry_run)
        print(json.dumps(result.to_dict(), ensure_ascii=False, indent=2))
        failed |= bool(result.errors) and result.collected == 0
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
