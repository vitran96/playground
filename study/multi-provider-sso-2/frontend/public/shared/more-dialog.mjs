// @ts-nocheck
import { html, render } from './lib.mjs'
import { ExplainPanel, ScoreChip, StatusChip, ChecklistSummary } from './components.mjs'
import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs'

mermaid.initialize({ startOnLoad: false })
const dialog = document.getElementById('more')
dialog.addEventListener('click', (e) => { if (e.target === dialog) dialog.close() })

const buttons = [...document.querySelectorAll('[data-arch]')]
const archs = await Promise.all(buttons.map(async (btn) => {
  const { explain } = await import(`../${btn.dataset.arch}/explain.mjs`)
  const card = btn.closest('article')
  const chip = document.createElement('span')
  card.querySelector('header strong').after(chip)
  render(html`<${ScoreChip} answers=${explain.checklist} /><${StatusChip} status=${explain.status} />`, chip)
  if (explain.status?.skip) card.style.opacity = '0.5'
  return { name: btn.dataset.arch, checklist: explain.checklist }
}))
render(html`<${ChecklistSummary} archs=${archs} />`, document.getElementById('summary'))

buttons.forEach((btn) => btn.addEventListener('click', async () => {
  const { explain } = await import(`../${btn.dataset.arch}/explain.mjs`)
  document.getElementById('more-title').textContent = explain.title
  document.getElementById('more-diagram').innerHTML = (await mermaid.render(`d-${btn.dataset.arch}`, explain.diagram)).svg
  render(html`<${ExplainPanel} explain=${explain} />`, document.getElementById('more-body'))
  dialog.showModal()
}))
