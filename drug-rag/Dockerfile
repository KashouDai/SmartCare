FROM python:3.11-slim

WORKDIR /app

# PROJECT_ROOT = Path(__file__).resolve().parents[3] 需解析为 /app，
# 使 data/、configs/、web/、catalog/ 的相对路径全部落在工作目录下。
ENV PYTHONPATH=/app/src \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./

# 默认走国内镜像加速（阿里云；清华镜像在本机网络下返回 403，故不采用）。
# 需要官方源时在构建时覆盖：
#   docker compose build --build-arg PIP_INDEX_URL=https://pypi.org/simple knowledge-assistant
ARG PIP_INDEX_URL=https://mirrors.aliyun.com/pypi/simple/
RUN pip install --no-cache-dir -i "${PIP_INDEX_URL}" -r requirements.txt

COPY src/ ./src/
COPY configs/ ./configs/
COPY web/ ./web/
COPY catalog/ ./catalog/
COPY data/chroma/ ./data/chroma/
COPY data/processed/drug_directory.json ./data/processed/drug_directory.json

EXPOSE 8000

CMD ["uvicorn", "drug_rag.api.main:app", "--host", "0.0.0.0", "--port", "8000"]
