import re


def chunk_markdown_file(markdown_text: str, max_chars: int = 800) -> list[str]:
    chunks: list[str] = []
    # Tách tài liệu thành các khối/đoạn văn dựa trên 2 lần xuống dòng
    blocks = markdown_text.split('\n\n')

    current_header = ""
    current_chunk = ""

    for block in blocks:
        block = block.strip()
        if not block:
            continue

        # 1. Nhận diện Header (Bắt đầu bằng #, ##, ###...)
        header_match = re.match(r'^(#{1,6})\s+(.*)', block)
        if header_match:
            # Nếu đang có chunk dang dở, lưu nó lại trước khi sang phần mới
            if current_chunk:
                chunks.append(current_chunk.strip())

            # Cập nhật Header hiện tại (Ngữ cảnh mỏ neo)
            current_header = block

            # Bắt đầu một chunk mới với Header này
            current_chunk = current_header + "\n"
            continue

        # 2. Xử lý các khối nội dung bình thường (Đoạn văn, Bảng, Bullet points)
        # Tính toán xem nếu nối thêm block này vào thì có vượt quá giới hạn không
        text_to_add = block if not current_chunk else "\n\n" + block

        if len(current_chunk) + len(text_to_add) <= max_chars:
            current_chunk += text_to_add
        else:
            # Nếu vượt quá giới hạn -> Lưu chunk hiện tại lại
            if current_chunk:
                chunks.append(current_chunk.strip())

            # Tạo chunk MỚI, KHÔNG QUÊN gắn lại cái Header cũ vào đầu chunk
            # Để AI đọc khúc sau vẫn biết là đang nằm trong mục nào
            current_chunk = (current_header + "\n\n" if current_header else "") + block

    # Lưu lại chunk cuối cùng nếu còn sót
    if current_chunk:
        chunks.append(current_chunk.strip())
    return chunks


def chunk_markdown_syntax(markdown_text: str, max_chars: int = 800) -> list[str]:
    chunks = []
    blocks = markdown_text.split('\n\n')

    current_header = ""
    current_chunk = ""

    for block in blocks:
        block = block.strip()
        if not block:
            continue

        header_match = re.match(r'^(#{1,6})\s+(.*)', block)
        if header_match:
            # --- CHỖ NÀY ĐÃ ĐƯỢC SỬA ---
            # Trước khi lưu chunk cũ, kiểm tra xem nó có thực sự chứa NỘI DUNG không.
            # Nếu nó quá ngắn (ví dụ < 20 ký tự) HOẶC chỉ toàn dấu '#' thì bỏ qua, không lưu.
            if current_chunk and len(
                    current_chunk.replace('\n', '').strip()) > 20 and not current_chunk.strip().startswith(
                    '#') and current_chunk.strip().count('#') < len(current_chunk.strip()) / 2:
                # Logic kiểm tra trên hơi dài, ta dùng cách đơn giản hơn:
                # Xóa hết dòng chứa '#' đi, xem phần còn lại có chữ nào không.
                lines = current_chunk.split('\n')
                content_lines = [l for l in lines if not l.strip().startswith('#')]
                if content_lines:  # Nếu có nội dung thực sự
                    chunks.append(current_chunk.strip())
            # -----------------------------

            current_header = block
            current_chunk = current_header + "\n"
            continue

        text_to_add = block if not current_chunk else "\n\n" + block

        if len(current_chunk) + len(text_to_add) <= max_chars:
            current_chunk += text_to_add
        else:
            # --- SỬA TƯƠNG TỰ Ở ĐÂY ---
            lines = current_chunk.split('\n')
            content_lines = [l for l in lines if not l.strip().startswith('#')]
            if content_lines:
                chunks.append(current_chunk.strip())
            # -----------------------------

            current_chunk = (current_header + "\n\n" if current_header else "") + block

    # Lưu chunk cuối cùng
    if current_chunk:
        lines = current_chunk.split('\n')
        content_lines = [l for l in lines if not l.strip().startswith('#')]
        if content_lines:
            chunks.append(current_chunk.strip())

    return chunks
