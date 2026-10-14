# Study log

## 13 Oct 2026

```shell
# Init
npx @angular/cli@17 new product-admin --style=scss --ssr=false --directory .

# Install dependencies
npx @angular/cli@17 add @angular/material@17

npx @angular/cli@17 add tailwindcss
npm i -D tailwindcss@3 postcss autoprefixer @tailwindcss/postcss

npm i @ngrx/store@17 @ngrx/effects@17 @ngrx/entity@17 @ngrx/store-devtools@17
npx @angular/cli@17 add @ngrx/schematics

# Generate environment
npx @angular/cli@17 g environments

# Generate boilerplate
# If path/to/save will generate into "to" with component name "save"
# If path/to/save/as will generate into "save" with component name "as"
npx @angular/cli@17 g service core/auth/auth
npx @angular/cli@17 g guard core/auth/auth
npx @angular/cli@17 g component features/home/page/home
npx @angular/cli@17 g service core/web-storage/cookie
npx @angular/cli@17 g service core/web-storage/localStorage
```

## 14 Oct 2026

- To redirect, must use `router` (similar to React)
- A guard can stop Angular continue render into the component
- Routing is similar to React
  - If I want different route with different layout, I must make different layout for each route
- To lazy load component, do `loadComponent: () => import('path/to/component').then(m => m.Component)`
