FROM docker.io/library/node:22-alpine

WORKDIR /app

COPY package.json ./
RUN npm install --omit=dev

COPY src ./src
COPY public ./public

CMD ["node", "src/server.js"]
