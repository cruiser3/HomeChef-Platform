import assert from 'node:assert/strict'
import test from 'node:test'
import { mainNavigation } from '../src/navigation.js'
import { createAuthGuard } from '../src/router/auth-guard.js'

test('main navigation defines five distinct household modules', () => {
  assert.deepEqual(
    mainNavigation.map(({ label }) => label),
    ['小家', '厨房', '购物车', '采购清单', '家庭'],
  )

  assert.equal(new Set(mainNavigation.map(({ path }) => path)).size, 5)
  assert.equal(new Set(mainNavigation.map(({ name }) => name)).size, 5)
})

test('unauthenticated users are redirected to login with the requested path', () => {
  const guard = createAuthGuard(() => null)
  const result = guard({
    fullPath: '/cart?from=tab',
    matched: [{ meta: { requiresAuth: true } }],
  })

  assert.deepEqual(result, {
    name: 'login',
    query: { redirect: '/cart?from=tab' },
  })
})

test('authenticated users can continue and login redirects stay within the app', () => {
  const guard = createAuthGuard(() => 'token')

  assert.equal(
    guard({ fullPath: '/orders', matched: [{ meta: { requiresAuth: true } }] }),
    true,
  )
  assert.equal(
    guard({ name: 'login', query: { redirect: '/orders' }, matched: [] }),
    '/orders',
  )
  assert.deepEqual(
    guard({ name: 'login', query: { redirect: '//outside.example' }, matched: [] }),
    { name: 'dishes' },
  )
})
