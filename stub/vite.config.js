export default {
  server: {
    host: '0.0.0.0',
    port: 5175,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        headers: {
          Origin: 'http://localhost:5173'
        }}
    }
  }
};

