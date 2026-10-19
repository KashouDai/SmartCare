FROM node:16-bullseye AS build

WORKDIR /app
COPY admin-web/hims-front-end-code-master/package*.json ./
RUN npm install
COPY admin-web/hims-front-end-code-master/ ./
RUN npm run build:prod

FROM nginx:1.25-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY admin-web/nginx.conf /etc/nginx/conf.d/default.conf

