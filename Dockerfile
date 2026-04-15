FROM python:3.12-slim

WORKDIR /app

ARG APP_VERSION=dev
ARG BUILD_SHA=unknown

ENV APP_VERSION=${APP_VERSION}
ENV BUILD_SHA=${BUILD_SHA}

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app/ ./app/

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
