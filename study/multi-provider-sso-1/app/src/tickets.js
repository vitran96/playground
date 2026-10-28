// DEMO SHORTCUT: real app assigns by internal user_id, never by email

const tickets = [
  {
    id: 'T-1',
    title: 'VPN access request',
    status: 'Open',
    assigneeEmail: 'alice@corp.example',
    comments: [
      { id: 'c1', type: 'shared', author: 'alice@corp.example', text: 'Request submitted for new VPN access profile.' },
      { id: 'c2', type: 'internal', author: 'bob@corp.example', text: 'Approved by IT Sec lead.' }
    ]
  },
  {
    id: 'T-2',
    title: 'Invoice mismatch',
    status: 'Waiting on partner',
    assigneeEmail: 'xavier@partner-a.example',
    comments: [
      { id: 'c3', type: 'shared', author: 'xavier@partner-a.example', text: 'Invoice #1092 does not match PO #44.' },
      { id: 'c4', type: 'internal', author: 'alice@corp.example', text: 'Billing team needs to adjust exchange rate.' }
    ]
  },
  {
    id: 'T-3',
    title: 'Shipping delay',
    status: 'Waiting on partner',
    assigneeEmail: 'yen@partner-b.example',
    comments: [
      { id: 'c5', type: 'shared', author: 'yen@partner-b.example', text: 'Shipment batch B-12 delayed at customs.' },
      { id: 'c6', type: 'internal', author: 'bob@corp.example', text: 'Carrier escalation filed with logistics provider.' }
    ]
  },
  {
    id: 'T-4',
    title: 'Security review',
    status: 'In progress',
    assigneeEmail: 'bob@corp.example',
    comments: [
      { id: 'c7', type: 'shared', author: 'bob@corp.example', text: 'Reviewing third-party integration specs.' },
      { id: 'c8', type: 'internal', author: 'alice@corp.example', text: 'Confidential: vendor audit findings pending.' }
    ]
  }
];

export function getTicketsForUser(userType, userEmail) {
  if (userType === 'internal') {
    return tickets;
  }
  // External user: only assigned to user's email, shared comments only
  return tickets
    .filter(t => t.assigneeEmail.toLowerCase() === (userEmail || '').toLowerCase())
    .map(t => ({
      ...t,
      comments: t.comments.filter(c => c.type === 'shared')
    }));
}

export function addInternalNote(ticketId, author, text) {
  const ticket = tickets.find(t => t.id === ticketId);
  if (!ticket) return null;
  const note = {
    id: `c${Date.now()}`,
    type: 'internal',
    author: author || 'Internal staff',
    text: text || ''
  };
  ticket.comments.push(note);
  return note;
}
