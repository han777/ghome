const fs = require('fs');
const file = 'src/views/mobile/Mine.vue';
let content = fs.readFileSync(file, 'utf-8');
content = content.replace("import { ref, onMounted, computed } from 'vue';", "import { ref, onMounted } from 'vue';");
fs.writeFileSync(file, content, 'utf-8');
console.log('Done');
