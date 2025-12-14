# Steppr Flow UI

Vue.js dashboard for monitoring Steppr Flow workflows.

## Overview

A modern, responsive dashboard built with Vue 3 and Tailwind CSS for monitoring and managing workflow executions.

## Features

- Real-time workflow monitoring
- Execution list with filtering and pagination
- Detailed execution view with step history
- Payload editor with inline editing
- Error handling with user-friendly messages
- Responsive design

## Tech Stack

- **Vue 3** - Composition API
- **Pinia** - State management
- **Vue Router** - Navigation
- **Tailwind CSS** - Styling
- **Vite** - Build tool
- **Axios** - HTTP client

## Development

```bash
# Install dependencies
npm install

# Start dev dashboard
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## Project Structure

```
stepprflow-ui/
├── src/
│   ├── components/       # Reusable Vue components
│   │   ├── PayloadEditor.vue
│   │   ├── PayloadNode.vue
│   │   └── StatusBadge.vue
│   ├── views/            # Page components
│   │   ├── Dashboard.vue
│   │   └── ExecutionDetail.vue
│   ├── stores/           # Pinia stores
│   │   └── workflow.js
│   ├── services/         # API services
│   │   └── api.js
│   ├── router/           # Vue Router config
│   └── App.vue
├── public/
├── index.html
├── vite.config.js
├── tailwind.config.js
└── package.json
```

## Configuration

The UI connects to the backend API via proxy in development:

**vite.config.js:**
```javascript
export default defineConfig({
  server: {
    proxy: {
      '/api': 'http://localhost:8090'
    }
  }
})
```

## Docker

```bash
# Build image
docker build -t stepprflow-ui .

# Run container
docker run -p 3000:80 stepprflow-ui
```

## Screenshots

### Dashboard
- Overview statistics
- Recent executions
- Quick filters by status

### Execution Detail
- Step-by-step progress
- Payload viewer/editor
- Error details
- Action buttons (Resume, Cancel)
