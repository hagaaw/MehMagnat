# ---------- Этап 1: сборка React-приложения ----------
FROM node:20-alpine AS build
WORKDIR /app

# Сначала только манифесты - так слой с зависимостями кэшируется
COPY package*.json ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

COPY . .
RUN npm run build

# ---------- Этап 2: nginx раздаёт статику и проксирует API ----------
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
