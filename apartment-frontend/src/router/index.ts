import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            name: 'Home',
            component: () => import('../views/Home.vue')
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
            meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
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
            meta: { requiresAuth: true, roles: ['ROLE_USER'] },
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
        const defaultPaths = ['/', '/admin', '/admin/dashboard', '/m', '/m/booking']
        const query = defaultPaths.includes(to.path) ? {} : { redirect: to.fullPath }
        next({ path: loginPath, query })
        return
    }

    if (to.meta.roles && token) {
        const roles: string[] = JSON.parse(localStorage.getItem('roles') || '[]')
        if (!roles.some(r => (to.meta.roles as string[]).includes(r))) {
            const isAdmin = roles.includes('ROLE_ADMIN')
            const isUser = roles.includes('ROLE_USER')
            if (isAdmin && isUser) next('/role-selection')
            else if (isAdmin) next('/admin')
            else if (isUser) next('/m')
            else next('/login')
            return
        }
    }

    next()
})

export default router
