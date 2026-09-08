# frontend/Dockerfile
FROM node:24-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install
COPY . .
# The production environment carries __API_URL__ / __KEYCLOAK_URL__ placeholders
# rather than hardcoded hosts, substituted here — Angular resolves its
# environment at build time, so there is nothing to change on the server
# afterwards.
ARG API_URL=http://localhost:8081
ARG KEYCLOAK_URL=http://localhost:8080
RUN sed -i "s#__API_URL__#${API_URL}#g; s#__KEYCLOAK_URL__#${KEYCLOAK_URL}#g" src/environment/environment.prod.ts \
    && ! grep -q "__API_URL__\|__KEYCLOAK_URL__" src/environment/environment.prod.ts
RUN npm run build -- --configuration=production

FROM nginx:alpine
WORKDIR /usr/share/nginx/html
# The application builder emits the browser bundle under dist/<project>/browser
COPY --from=builder /app/dist/clinic-frontend/browser/ .
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
