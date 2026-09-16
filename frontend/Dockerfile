FROM node:18-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install --no-audit --no-fund
COPY . .
# Build assumes the API base paths work in production. In production we
# configure nginx to proxy /api/* to the backend services via Docker
# networking. Adjust proxy_pass targets in nginx.conf if the env changes.
RUN npm run build

FROM nginx:1.25-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
