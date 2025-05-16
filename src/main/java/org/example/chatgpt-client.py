"""
mcp_slack_client.py
List every Slack channel whose name starts with “incident-”, pull its
messages via the MCP Slack server, and save each channel’s history to
<channel>_messages.json.
"""

import asyncio, json, os, re
from pathlib import Path
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client


# ──────────────────────────────────────────────────────────────────────────────
# Configuration – set real values before you run!
# ──────────────────────────────────────────────────────────────────────────────
SLACK_BOT_TOKEN      = os.getenv("SLACK_BOT_TOKEN")      # xoxb-…
SLACK_SIGNING_SECRET = os.getenv("SLACK_SIGNING_SECRET") # if your server needs it
SLACK_APP_TOKEN      = os.getenv("SLACK_APP_TOKEN")      # xapp-…
SLACK_TEAM_ID        = os.getenv("SLACK_TEAM_ID")        # T12345678

# Docker image that contains the MCP Slack server
IMAGE = "mcp/slack:latest"

# Where to drop JSON output
OUT_DIR = Path("slack_exports")
OUT_DIR.mkdir(exist_ok=True)


# ──────────────────────────────────────────────────────────────────────────────
# Core logic
# ──────────────────────────────────────────────────────────────────────────────
async def main() -> None:
    """
    1. Spin up the MCP Slack server inside Docker with stdio transport.
    2. Initialise a ClientSession.
    3. list_resources → filter channels whose name starts with 'incident'.
    4. read_resource for each channel and dump messages to disk.
    """

    # 1️⃣ Build parameters for stdio transport that launches Docker
    server_params = StdioServerParameters(
        command="docker",
        args=[
            "run", "--rm", "-i",          # -i is needed so stdio stays open
            "-e", f"SLACK_BOT_TOKEN={SLACK_BOT_TOKEN}",
            "-e", f"SLACK_SIGNING_SECRET={SLACK_SIGNING_SECRET}",
            "-e", f"SLACK_APP_TOKEN={SLACK_APP_TOKEN}",
            "-e", f"SLACK_TEAM_ID={SLACK_TEAM_ID}",
            IMAGE,
        ],
    )

    # 2️⃣ Open stdio transport and create a ClientSession
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()

            # 3️⃣ Ask the server for every resource it exposes
            res_list = await session.list_resources()
            channels = [
                r
                for r in res_list.resources
                if r.uri.startswith("slack://channel/")
                and r.name
                and r.name.lower().startswith("incident")
            ]

            print(f"Found {len(channels)} incident channels")

            # 4️⃣ Read each channel’s history and save to disk
            for ch in channels:
                print(f"⏳  downloading #{ch.name} …")
                data = await session.read_resource(ch.uri)
                msgs = data.contents  # list[Any]

                safe_name = re.sub(r"[^0-9A-Za-z_-]", "_", ch.name)
                outfile = OUT_DIR / f"{safe_name}_messages.json"
                with outfile.open("w", encoding="utf-8") as fp:
                    json.dump([m for m in msgs], fp, indent=2, ensure_ascii=False)

                print(f"   → saved {len(msgs)} messages to {outfile}")

            print("✅  done – closing connection")


if __name__ == "__main__":
    # asyncio.run() gives us a clean event-loop even in notebooks / REPLs
    asyncio.run(main())
