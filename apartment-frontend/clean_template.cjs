const fs = require('fs');
const file = 'src/views/mobile/OrderDetail.vue';
let content = fs.readFileSync(file, 'utf-8');

// Find last </template> before <script
const scriptIdx = content.indexOf('<script');
const templateSection = content.substring(0, scriptIdx);

// Find the LAST </template> in the template section
const lastTemplateEnd = templateSection.lastIndexOf('</template>') + '</template>'.length;

// Keep: new template (already starts at 0), script and style
const afterTemplate = content.substring(lastTemplateEnd);

// afterTemplate should start with \n\r\n<script...
console.log('After template starts with:', JSON.stringify(afterTemplate.substring(0, 30)));

// Build final: new template stays as-is (it's already the first </template> at line 195)
// We need to remove everything between line 195 </template> and line 236 </template>

// Find all </template> positions
let pos = 0;
const positions = [];
while (true) {
  const idx = content.indexOf('</template>', pos);
  if (idx === -1) break;
  positions.push(idx);
  pos = idx + 1;
}
console.log('</template> positions:', positions);

// The first </template> is the new template end (our fresh template)
// Everything between positions[0] and positions[positions.length-1] is duplicated junk
// Keep: content[0..positions[0]+11] + content after last </template>

const firstEnd = positions[0] + '</template>'.length;
const lastEnd = positions[positions.length - 1] + '</template>'.length;

const cleaned = content.substring(0, firstEnd) + content.substring(lastEnd);
fs.writeFileSync(file, cleaned, 'utf-8');
console.log('Cleaned. Lines:', cleaned.split('\n').length);
