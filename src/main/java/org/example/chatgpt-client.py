#!/usr/bin/env python3
"""
mcp_slack_dump.py
─────────────────
• Starts the Slack MCP server inside Docker (image mcp/slack:latest).
• Lists every Slack channel whose name starts with “incident”.
• Downloads up to 1 000 messages per channel via the *tool* API.
• Writes each channel’s history to  slack_exports/<channel>_messages.json
Tested with:
  · Python 3.11 / 3.12 / 3.13
  · mcp 0.9.2
"""

import asyncio
import json
import os
import re
from pathlib import Path

from mcp import ClientSession                        # stable path
from mcp.client.stdio import stdio_client, StdioServerParameters

# ─────────── configuration ────────────
IMAGE          = "mcp/slack:latest"        # public Docker-Hub tag
OUT_DIR        = Path("slack_exports")
OUT_DIR.mkdir(exist_ok=True)

# Slack creds must be real values:
SLACK_BOT_TOKEN      = os.getenv("SLACK_BOT_TOKEN")      # xoxb-…
SLACK_TEAM_ID        = os.getenv("SLACK_TEAM_ID")        # T12345678
SLACK_SIGNING_SECRET = os.getenv("SLACK_SIGNING_SECRET", "")
SLACK_APP_TOKEN      = os.getenv("SLACK_APP_TOKEN", "")

# ─────────── helpers ────────────
def safe_filename(name: str) -> str:
    return re.sub(r"[^0-9A-Za-z_-]", "_", name)

def dump(channel: str, msgs: list[dict]) -> None:
    fp = OUT_DIR / f"{safe_filename(channel)}_messages.json"
    fp.write_text(json.dumps(msgs, indent=2, ensure_ascii=False))
    print(f"📄  {len(msgs):4} messages → {fp}")

# ─────────── main async entrypoint ────────────
async def main() -> None:
    # 1️⃣ parameters for stdio transport (Docker inside)
    params = StdioServerParameters(
        command="docker",
        args=[
            "run", "--rm", "-i",                # -i keeps stdio open
            "-e", f"SLACK_BOT_TOKEN={SLACK_BOT_TOKEN}",
            "-e", f"SLACK_TEAM_ID={SLACK_TEAM_ID}",
            "-e", f"SLACK_SIGNING_SECRET={SLACK_SIGNING_SECRET}",
            "-e", f"SLACK_APP_TOKEN={SLACK_APP_TOKEN}",
            IMAGE,
        ],
    )

    # 2️⃣ open transport + client session
    async with stdio_client(params) as (read, write):
        async with ClientSession(read, write) as ses:
            await ses.initialize()
            # ---- TOOL discovery -------------------------------------------
            tool_list = await ses.list_tools()
            tools = {t.name: t for t in tool_list.tools}

            list_channels_tool = next(
                t for t in tools.values()
                if t.name.startswith("slack_list_channels")
            )
            history_tool = next(
                t for t in tools.values()
                if t.name.startswith(("slack_get_channel_history",
                                      "slack_list_messages"))
            )

            # ---- step 1: get all channels ---------------------------------
            ch_resp = await ses.execute_tool(list_channels_tool.tool_id, {})
            channels = [
                c for c in ch_resp["channels"]
                if c["name"].lower().startswith("incident")
            ]
            print(f"🔎  found {len(channels)} incident channels")

            # ---- step 2: dump each channel’s history ----------------------
            for ch in channels:
                print(f"⏳  downloading #{ch['name']}")
                hist = await ses.execute_tool(
                    history_tool.tool_id,
                    {"channel_id": ch["id"], "limit": 1000},
                )
                dump(ch["name"], hist["messages"])

            print("✅  finished – closing connection")

# ─────────── kick off event-loop ────────────
if __name__ == "__main__":
    if not (SLACK_BOT_TOKEN and SLACK_TEAM_ID):
        raise SystemExit(
            "⚠️  Set SLACK_BOT_TOKEN and SLACK_TEAM_ID in your environment first."
        )
    asyncio.run(main())
