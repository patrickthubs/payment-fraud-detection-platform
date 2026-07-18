FROM node:26-alpine AS build
WORKDIR /workspace

RUN npm install --global npm@11.17.0

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.29-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/ui/browser /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=15s --timeout=5s --retries=5 \
    CMD wget --quiet --tries=1 --spider http://127.0.0.1/health || exit 1
