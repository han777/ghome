import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            redirect: '/admin'
        },
        {
            path: '/login',
            name: 'Login',
            component: () => import('../views/Login.vue')
        },
        {
            path: '/role-selection',
            name: 'RoleSelection',
            component: () => import('../views/RoleSelection.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/admin',
            name: 'Admin',
            component: () => import('../views/admin/Layout.vue'),
            meta: { requiresAuth: true },
            redirect: '/admin/dashboard',
            children: [
                {
                    path: 'dashboard',
                    name: 'Dashboard',
                    component: () => import('../views/admin/Dashboard.vue')
                },
                {
                    path: 'accounts',
                    name: 'Accounts',
                    component: () => import('../views/admin/Accounts.vue')
                },
                {
                    path: 'roles',
                    name: 'Roles',
                    component: () => import('../views/admin/Roles.vue')
                },
                {
                    path: 'menus',
                    name: 'Menus',
                    component: () => import('../views/admin/Menus.vue')
                },
                {
                    path: 'dicts',
                    name: 'Dicts',
                    component: () => import('../views/admin/Dicts.vue')
                },
                {
                    path: 'depts',
                    name: 'Depts',
                    component: () => import('../views/admin/Depts.vue')
                },
                {
                    path: 'rooms',
                    name: 'Rooms',
                    component: () => import('../views/admin/Rooms.vue')
                },
                {
                    path: 'orders',
                    name: 'Orders',
                    component: () => import('../views/admin/Orders.vue')
                },
                {
                    path: 'history-orders',
                    name: 'HistoryOrders',
                    component: () => import('../views/admin/HistoryOrders.vue')
                },
                {
                    path: 'buildings',
                    name: 'Buildings',
                    component: () => import('../views/admin/Buildings.vue')
                },
                {
                    path: 'room-types',
                    name: 'RoomTypes',
                    component: () => import('../views/admin/RoomTypes.vue')
                },
                {
                    path: 'gantt',
                    name: 'Gantt',
                    component: () => import('../views/admin/Gantt.vue')
                },
                {
                    path: 'reports',
                    name: 'Reports',
                    component: () => import('../views/admin/Reports.vue')
                },
                {
                    path: 'room-type-forecast',
                    name: 'RoomTypeForecast',
                    component: () => import('../views/admin/RoomTypeForecast.vue')
                },
                {
                    path: 'room-fee-detail',
                    name: 'RoomFeeDetail',
                    component: () => import('../views/admin/RoomFeeDetail.vue')
                },
                {
                    path: 'service-fee-detail',
                    name: 'ServiceFeeDetail',
                    component: () => import('../views/admin/ServiceFeeDetail.vue')
                },
                {
                    path: 'product-prices',
                    name: 'ProductPrices',
                    component: () => import('../views/admin/ProductPrices.vue')
                },
                {
                    path: 'maintenances',
                    name: 'Maintenances',
                    component: () => import('../views/admin/Maintenances.vue')
                }
            ]
        },
        {
            path: '/m',
            component: () => import('../views/mobile/MobileLayout.vue'),
            redirect: '/m/booking',
            meta: { requiresAuth: true },
            children: [
                {
                    path: 'booking',
                    name: 'MobileBooking',
                    component: () => import('../views/mobile/Booking.vue')
                },
                {
                    path: 'records',
                    name: 'MobileRecords',
                    component: () => import('../views/mobile/Records.vue')
                },
                {
                    path: 'mine',
                    name: 'MobileMine',
                    component: () => import('../views/mobile/Mine.vue')
                }
            ]
        },
        {
            path: '/m/auth',
            name: 'MobileAuth',
            component: () => import('../views/mobile/Auth.vue')
        },
        {
            path: '/m/auth/callback',
            name: 'MobileWecomCallback',
            component: () => import('../views/mobile/WecomCallback.vue')
        },
        {
            path: '/m/room-select',
            name: 'MobileRoomSelect',
            component: () => import('../views/mobile/RoomSelect.vue')
        },
        {
            path: '/m/confirm',
            name: 'MobileConfirm',
            component: () => import('../views/mobile/Confirm.vue')
        },
        {
            path: '/m/order-detail/:id',
            name: 'MobileOrderDetail',
            component: () => import('../views/mobile/OrderDetail.vue'),
            meta: { requiresAuth: true }
        }
    ]
})

router.beforeEach((to, _from, next) => {
    const token = localStorage.getItem('token')
    if (to.meta.requiresAuth && !token) {
        const loginPath = to.path.startsWith('/m/') ? '/m/auth' : '/login'
        next({ path: loginPath, query: { redirect: to.fullPath } })
    } else {
        next()
    }
})

export default router
