import re
from typing import List, Dict


def chunk_markdown_by_headings(markdown_text: str) -> List[Dict]:
    lines = markdown_text.splitlines()

    chunks = []
    heading_stack = []
    current_heading = None
    current_level = None
    current_content = []

    heading_pattern = re.compile(r"^(#{1,6})\s+(.*)$")

    def save_current_chunk():
        if current_heading and current_content:
            content = "\n".join(current_content).strip()
            if content:
                chunks.append({
                    "title": current_heading,
                    "level": current_level,
                    "path": " > ".join([h["title"] for h in heading_stack]),
                    "content": content
                })

    for line in lines:
        match = heading_pattern.match(line)

        if match:
            save_current_chunk()

            hashes, title = match.groups()
            level = len(hashes)

            while heading_stack and heading_stack[-1]["level"] >= level:
                heading_stack.pop()

            heading_stack.append({
                "level": level,
                "title": title.strip()
            })

            current_heading = title.strip()
            current_level = level
            current_content = []

        else:
            current_content.append(line)

    save_current_chunk()

    return chunks
