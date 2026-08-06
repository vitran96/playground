import { useEffect, useState } from 'react';
import 'antd/dist/reset.css';
import '@gorules/jdm-editor/dist/style.css';
import './App.css'
import { DecisionGraph, JdmConfigProvider, type DecisionGraphType } from '@gorules/jdm-editor';

type Rule = { id: number; name: string; decision: string };

function ListView({ onOpen, onNew }: { onOpen: (id: number) => void; onNew: () => void }) {
  const [rules, setRules] = useState<Rule[]>([]);

  const load = () => {
    fetch('/api/rules').then((r) => r.json()).then(setRules);
  };

  useEffect(load, []);

  const handleDelete = async (id: number) => {
    await fetch(`/api/rules/${id}`, { method: 'DELETE' });
    load();
  };

  return (
    <div style={{ padding: 20 }}>
      <button onClick={onNew}>+ New Decision</button>
      <ul>
        {rules.map((r) => (
          <li key={r.id}>
            <span onClick={() => onOpen(r.id)} style={{ cursor: 'pointer' }}>
              {r.name}
            </span>
            {' '}
            <button onClick={() => handleDelete(r.id)}>Delete</button>
          </li>
        ))}
      </ul>
    </div>
  );
}

function DetailView({ id, onBack }: { id: number | null; onBack: () => void }) {
  const [graph, setGraph] = useState<DecisionGraphType>({ nodes: [], edges: [] });
  const [name, setName] = useState<string>('');

  useEffect(() => {
    if (id === null) return;
    fetch(`/api/rules/${id}`)
      .then((r) => r.json())
      .then((rule: Rule) => {
        setName(rule.name);
        setGraph(JSON.parse(rule.decision));
      });
  }, [id]);

  const handleSave = async () => {
    let ruleName = name;
    if (id === null && !ruleName) {
      ruleName = window.prompt('Decision name:') ?? '';
      if (!ruleName) return;
      setName(ruleName);
    }

    const body = JSON.stringify({ name: ruleName, decision: JSON.stringify(graph) });

    if (id === null) {
      await fetch('/api/rules', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body });
    } else {
      await fetch(`/api/rules/${id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body });
    }
    onBack();
  };

  return (
    <div style={{ padding: 20, height: '90vh' }}>
      <button onClick={onBack}>← Back</button>
      <button onClick={handleSave}>Save</button>
      <div style={{ height: '100%' }}>
        <JdmConfigProvider>
          <DecisionGraph value={graph} onChange={setGraph} />
        </JdmConfigProvider>
      </div>
    </div>
  );
}

export default function App() {
  const [view, setView] = useState<'list' | 'detail'>('list');
  const [selectedId, setSelectedId] = useState<number | null>(null);

  if (view === 'detail') {
    return (
      <DetailView
        id={selectedId}
        onBack={() => {
          setView('list');
          setSelectedId(null);
        }}
      />
    );
  }

  return (
    <ListView
      onOpen={(id) => {
        setSelectedId(id);
        setView('detail');
      }}
      onNew={() => {
        setSelectedId(null);
        setView('detail');
      }}
    />
  );
}