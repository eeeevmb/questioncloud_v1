######################  Stage-1：构建静态文件  ######################
FROM node:20-alpine AS builder
WORKDIR /app

COPY pnpm-lock.yaml package.json ./
RUN npm i -g pnpm && pnpm i --frozen-lockfile

COPY . .
RUN pnpm run build

######################  Stage-2：Nginx  #####################
FROM nginx:1.27-alpine
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx","-g","daemon off;"]
