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
    const alert = wrapper.get('[role="alert"]')
    expect(alert.text()).toBe('Kaydedildi')
    expect(alert.classes()).toContain('ok')
  })
})
