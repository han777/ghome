const fs = require('fs');
const path = require('path');

const viewsDir = path.join(__dirname, 'src', 'views', 'admin');
const layoutDir = path.join(__dirname, 'src', 'layout');

const dictionary = {
  // Headers and Labels
  '>Code<': '>代码<',
  '>Name \\(Multi-lang\\)<': '>名称 (多语言)<',
  '>Short Rent<': '>短租<',
  '>Long Rent<': '>长租<',
  '>Type Code<': '>类型代码<',
  '>Names \\(JSON Map\\)<': '>名称 (多语言JSON)<',
  '>Short Rent Price<': '>短租价格<',
  '>Long Rent Price<': '>长租价格<',
  '>Images \\(9-grid display\\)<': '>图片 (九宫格展示)<',
  '>Room No.<': '>房间号<',
  '>Location<': '>位置<',
  '>Direction<': '>朝向<',
  '>Area \\(㎡\\)<': '>面积 (㎡)<',
  '>Location \\(Floor\\)<': '>位置 (楼层)<',
  '>Created Time<': '>创建时间<',
  '>Total Price<': '>总价<',
  '>Products<': '>产品<',
  '>Service Cost<': '>服务费用<',
  '>Contact Phone<': '>联系电话<',
  '>Check-in User<': '>入住人<',
  '>Occupant Name<': '>入住人姓名<',
  '>ID Number<': '>身份证号<',
  '>Add Occupant<': '>添加居住人<',
  '>Date Range<': '>日期范围<',
  '>Building<': '>楼栋<',
  '>Building Code<': '>楼栋代码<',
  '>Building Name<': '>楼栋名称<',
  '>Number of Floors<': '>楼层数<',
  '>Total Rooms<': '>总房间数<',
  '>Dict Code<': '>字典编码<',
  '>Dict Name<': '>字典名称<',
  '>Item Code<': '>项编码<',
  '>Item Value<': '>项值<',
  '>Order List<': '>订单列表<',
  '>Room Status<': '>房态<',
  '>New Order<': '>新订单<',
  '>Quick Booking<': '>快速预订<',
  '>View All Orders<': '>查看所有订单<',
  '>Recent Orders<': '>最近订单<',
  '>Maintenance Statistics<': '>维修统计<',
  '>Maintain<': '>维修<',
  '>Room Number<': '>房间号<',
  '>Start Date<': '>开始日期<',
  '>End Date<': '>结束日期<',
  '>Department<': '>部门<',
  '>Dept Code<': '>部门代码<',
  '>Dept Name<': '>部门名称<',
  '>Manager<': '>经理<',
  '>Contact Email<': '>联系邮箱<',
  '>Contact Number<': '>联系电话<',
  '>Remark<': '>备注<',
  '>Value<': '>值<',
  '>Guest<': '>客人<',

  // Select Options & Placeholders
  'e.g. KING, SUITE': '如 KING, SUITE',
  '>Male<': '>男<',
  '>Female<': '>女<',
  '>Personal<': '>个人<',
  '>Group<': '>团队<',

  // Filter Buttons
  '>All<': '>全部<',
  '>Available<': '>空闲<',
  '>Occupied<': '>已入住<',
  '>Locked<': '>锁定<',

  // Buttons with symbols
  '>🎴 Card<': '>🎴 卡片<',
  '>📋 Table<': '>📋 表格<'
};

function translateFile(filePath) {
  let content = fs.readFileSync(filePath, 'utf-8');
  let original = content;

  for (const [key, value] of Object.entries(dictionary)) {
    const regex = new RegExp(key, 'g');
    content = content.replace(regex, value);
  }

  if (content !== original) {
    fs.writeFileSync(filePath, content, 'utf-8');
    console.log(`Translated ${path.basename(filePath)}`);
  }
}

const viewsFiles = fs.readdirSync(viewsDir).filter(f => f.endsWith('.vue'));
for (const file of viewsFiles) {
  translateFile(path.join(viewsDir, file));
}

if (fs.existsSync(layoutDir)) {
  const layoutFiles = fs.readdirSync(layoutDir).filter(f => f.endsWith('.vue'));
  for (const file of layoutFiles) {
    translateFile(path.join(layoutDir, file));
  }
}

