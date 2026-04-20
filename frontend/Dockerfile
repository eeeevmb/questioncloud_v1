######################  Stage-1：构建静态文件  ######################
FROM node:20-alpine AS builder
WORKDIR /app

ARG NPM_REGISTRY=https://registry.npmjs.org/

COPY package.json package-lock.json ./
RUN npm config set registry "${NPM_REGISTRY}" \
    && npm config set fetch-retries 5 \
    && npm config set fetch-retry-factor 2 \
    && npm config set fetch-retry-mintimeout 20000 \
    && npm config set fetch-retry-maxtimeout 120000 \
    && npm config set fetch-timeout 180000 \
    && set -eux; \
    for i in 1 2 3; do \
      npm ci --no-audit --no-fund && exit 0; \
      echo "npm ci failed, retry ${i}/3"; \
      sleep 5; \
    done; \
    exit 1

COPY . .
RUN npm run build

######################  Stage-2：Nginx  #####################
FROM nginx:1.27-alpine
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx","-g","daemon off;"]
