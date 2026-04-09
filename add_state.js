const fs = require(`fs`);
const content = fs.readFileSync(`frontend/src/views/PaperDetailView.vue`, `utf8`);

// Add replaceLoading state
let updated = content.replace(
  `const previewLoading = ref(false);
const submittingItems = ref(false);`,
  `const previewLoading = ref(false);
const submittingItems = ref(false);
const replaceLoading = ref(false);`
);

fs.writeFileSync(`frontend/src/views/PaperDetailView.vue`, updated, `utf8`);
console.log(`Added replaceLoading state`);
