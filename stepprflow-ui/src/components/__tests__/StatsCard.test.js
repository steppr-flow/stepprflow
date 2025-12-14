import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import StatsCard from '../StatsCard.vue'

describe('StatsCard', () => {
  let rafCallbacks = []
  let time = 0

  beforeEach(() => {
    rafCallbacks = []
    time = 0

    // Mock performance.now to return controllable time
    vi.stubGlobal('performance', {
      now: () => time
    })

    // Mock requestAnimationFrame to collect callbacks
    vi.stubGlobal('requestAnimationFrame', (cb) => {
      rafCallbacks.push(cb)
      return rafCallbacks.length
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  // Helper to run animation frames
  const runAnimationFrames = (frames, timeIncrement = 100) => {
    for (let i = 0; i < frames; i++) {
      time += timeIncrement
      const callbacks = [...rafCallbacks]
      rafCallbacks = []
      callbacks.forEach(cb => cb(time))
    }
  }

  describe('rendering', () => {
    it('renders label', () => {
      const wrapper = mount(StatsCard, {
        props: { label: 'Total Workflows', value: 100 }
      })

      expect(wrapper.text()).toContain('Total Workflows')
    })

    it('renders value after animation', async () => {
      const wrapper = mount(StatsCard, {
        props: { label: 'Active', value: 10 }
      })

      // Run animation to completion (500ms duration)
      runAnimationFrames(10, 100)

      // Wait for Vue to update the DOM
      await wrapper.vm.$nextTick()

      expect(wrapper.text()).toContain('10')
    })
  })

  describe('variants', () => {
    it('applies success color class for success variant', () => {
      const wrapper = mount(StatsCard, {
        props: { label: 'Completed', value: 100, variant: 'success' }
      })

      expect(wrapper.find('.text-emerald-600').exists()).toBe(true)
    })

    it('applies danger color class for danger variant', () => {
      const wrapper = mount(StatsCard, {
        props: { label: 'Failed', value: 5, variant: 'danger' }
      })

      expect(wrapper.find('.text-red-600').exists()).toBe(true)
    })

    it('applies primary color class for primary variant', () => {
      const wrapper = mount(StatsCard, {
        props: { label: 'Total', value: 50, variant: 'primary' }
      })

      expect(wrapper.find('.text-primary-600').exists()).toBe(true)
    })

    it('applies warning color class for warning variant', () => {
      const wrapper = mount(StatsCard, {
        props: { label: 'Pending', value: 15, variant: 'warning' }
      })

      expect(wrapper.find('.text-amber-600').exists()).toBe(true)
    })

    it('applies info color class for info variant', () => {
      const wrapper = mount(StatsCard, {
        props: { label: 'Running', value: 3, variant: 'info' }
      })

      expect(wrapper.find('.text-sky-600').exists()).toBe(true)
    })

    it('applies default color class for default variant', () => {
      const wrapper = mount(StatsCard, {
        props: { label: 'Items', value: 25, variant: 'default' }
      })

      expect(wrapper.find('.text-gray-700').exists()).toBe(true)
    })

    it('applies default color class when variant is not specified', () => {
      const wrapper = mount(StatsCard, {
        props: { label: 'Items', value: 25 }
      })

      expect(wrapper.find('.text-gray-700').exists()).toBe(true)
    })
  })

  describe('value animation', () => {
    it('starts with animated value at 0', () => {
      const wrapper = mount(StatsCard, {
        props: { label: 'Test', value: 100 }
      })

      expect(wrapper.vm.animatedValue).toBe(0)
    })

    it('animates to target value', () => {
      const wrapper = mount(StatsCard, {
        props: { label: 'Test', value: 50 }
      })

      // Run animation to completion
      runAnimationFrames(10, 100)

      expect(wrapper.vm.animatedValue).toBe(50)
    })
  })
})
