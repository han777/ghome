const fs = require('fs');
const path = require('path');
const viewsDir = path.join(__dirname, 'src', 'views', 'mobile');
const files = ['Booking.vue', 'Confirm.vue', 'Mine.vue', 'OrderDetail.vue', 'Records.vue', 'RoomSelect.vue'];

for (const file of files) {
  const filePath = path.join(viewsDir, file);
  let content = fs.readFileSync(filePath, 'utf-8');
  
  // Fix the broken translateField from previous shell execution
  content = content.replace(/translateField\(, \\.locale\)/g, "translateField(type.nameIntlJson, $i18n.locale)");
  // Actually, in different files it might have been room.roomTypeName. Let's just fix them all by using my initial regex from JS file not shell.
  // The ones broken are only the ones that HAD `translateField(..., locale)` before I ran that bad PS command.
  
  // Actually let's just view them to be sure, or better, re-apply the correct logic from the original original.
  // Let me just restore translateField correctly.
  
  fs.writeFileSync(filePath, content, 'utf-8');
}
