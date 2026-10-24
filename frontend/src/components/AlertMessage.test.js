import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AlertMessage from './AlertMessage.vue'

describe('AlertMessage', () => {
  it('stays empty until there is a message', () => {
    const wrapper = mount(AlertMessage)
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  })

  it('shows the message and the ok tone', () => {
    const wrapper = mount(AlertMessage, { props: { message: 'Kaydedildi', tone: 'ok' } })
    const banner = wrapper.get('[role="status"]')
    expect(banner.text()).toBe('Kaydedildi')
    expect(banner.classes()).toContain('ok')
  })
})
