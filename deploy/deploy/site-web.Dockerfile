FROM node:16-bullseye AS build

WORKDIR /app
COPY patient-web/hims-site/package*.json ./
RUN npm install
COPY patient-web/hims-site/ ./
RUN npm run build

FROM node:16-bullseye-slim

WORKDIR /app
ENV HOST=0.0.0.0 PORT=3000 NUXT_API_BASE_URL=/api
COPY --from=build /app/package*.json ./
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/.nuxt ./.nuxt
COPY --from=build /app/static ./static
COPY --from=build /app/nuxt.config.js ./nuxt.config.js

EXPOSE 3000
CMD ["npm", "run", "start"]

