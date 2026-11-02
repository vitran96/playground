# Backend service running Hono on Node 22 (Used by Architectures 1, 2, 3, 4)
FROM docker.io/library/node:22-alpine

WORKDIR /app

COPY package.json ./
RUN npm install

COPY tsconfig.json ./
COPY src/ ./src/

EXPOSE 3000
CMD ["npm", "start"]
