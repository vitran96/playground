import { useEffect, useState } from 'react';
import 'antd/dist/reset.css';
import '@gorules/jdm-editor/dist/style.css';
import './App.css'
import { DecisionGraph, GraphSimulator, JdmConfigProvider, type DecisionGraphType, type Simulation } from '@gorules/jdm-editor';
import { PlayCircleOutlined } from '@ant-design/icons';

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
            <span>{r.name}</span>
            {' '}
            <button onClick={() => onOpen(r.id)}>Open</button>
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
  const [nameError, setNameError] = useState(false);
  const [simulation, setSimulation] = useState<Simulation>();
  const [ruleId, setRuleId] = useState<number | null>(id);
  const [contextInput, setContextInput] = useState<string>('{}');

  useEffect(() => {
    if (id === null) return;
    fetch(`/api/rules/${id}`)
      .then((r) => r.json())
      .then((rule: Rule) => {
        setName(rule.name);
        setGraph(JSON.parse(rule.decision));
      });
  }, [id]);

  // Saves current graph, returns the rule id to use (existing or newly created).
  // Returns null if validation fails (new rule, no name).
  const saveRule = async (currentGraph: DecisionGraphType): Promise<number | null> => {
    if (ruleId === null && !name.trim()) {
      setNameError(true);
      return null;
    }
    setNameError(false);

    const body = JSON.stringify({ name, decision: JSON.stringify(currentGraph) });

    if (ruleId === null) {
      const res = await fetch('/api/rules', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body });
      const created: Rule = await res.json();
      setRuleId(created.id);
      return created.id;
    } else {
      await fetch(`/api/rules/${ruleId}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body });
      return ruleId;
    }
  };

  const handleSave = async () => {
    const savedId = await saveRule(graph);
    if (savedId === null) return;
    onBack();
  };

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const handleRun = async ({ graph: runGraph }: any) => {
    setGraph(runGraph);
    const savedId = await saveRule(runGraph);
    if (savedId === null) return;

    const res = await fetch(`/api/rules/${savedId}/simulate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: contextInput,
    });
    const result = await res.json();
    setSimulation({ result: { ...result, snapshot: runGraph } });
  };

  return (
    <div style={{ padding: 20, height: '90vh' }}>
      <button onClick={onBack}>← Back</button>
      {ruleId === null && (
        <input
          placeholder="Decision name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          style={{ marginLeft: 10, borderColor: nameError ? 'red' : undefined }}
        />
      )}
      <button onClick={handleSave}>Save</button>
      <div style={{ margin: '10px 0' }}>
        <label>
          Input JSON:
          <br />
          <textarea
            value={contextInput}
            onChange={(e) => setContextInput(e.target.value)}
            rows={4}
            style={{ width: '100%', fontFamily: 'monospace' }}
          />
        </label>
      </div>
      <div style={{ height: '100%' }}>
        <JdmConfigProvider>
          <DecisionGraph
            value={graph}
            onChange={setGraph}
            simulate={simulation}
            defaultActivePanel="simulator"
            panels={[
              {
                id: 'simulator',
                title: 'Simulator',
                icon: <PlayCircleOutlined />,
                renderPanel: () => (
                  <GraphSimulator onRun={handleRun} onClear={() => setSimulation(undefined)} />
                ),
              },
            ]}
          />
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