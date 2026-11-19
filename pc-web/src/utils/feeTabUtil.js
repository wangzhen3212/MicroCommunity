export function openFeeTabPane(vm, tabName, params, delay) {
  const timeout = delay === undefined ? 500 : delay
  setTimeout(() => {
    const pane = vm.$refs[tabName]
    if (pane && typeof pane.open === 'function') {
      pane.open(params)
    }
  }, timeout)
}
