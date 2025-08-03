TEST_CASES = [
    {
        'input': {
            'N': 6,
            'A': [1, 2, 4, 5],
            'B': [2, 3, 5, 6]
        },
        'expected': 2
    },
    {
        'input': {
            'N': 4,
            'A': [1, 2, 3, 3],
            'B': [2, 3, 1, 4]
        },
        'expected': 4
    },
    {
        'input': {
            'N': 5,
            'A': [1, 1, 1],
            'B': [2, 3, 4]
        },
        'expected': 1
    },
    {
        'input': {
            'N': 3,
            'A': [],
            'B': []
        },
        'expected': 0
    }
]

class Node:
    def __init__(self, num):
        self.num = num
        self.nodes = set()

    def __eq__(self, other): 
        if not isinstance(other, 'Node'):
            # don't attempt to compare against unrelated types
            return NotImplemented

        return self.num == other.num
    
    def __hash__(self):
        return hash(self.num)
    
    def connect(self, other):
        self.nodes.add(other)

def count_max_edge(node):
    longest = 0

    visited = set()


    # BFS
    nodes = []
    nodes.append((node, 0))
    while len(nodes) > 0:
        curr = nodes.pop(0)
        curr_node = curr[0]
        counter = curr[1]
        # print('curr ', curr_node.num)
        if curr_node not in visited:
            visited.add(curr_node)
            next_count = counter + 1
            next_nodes = [(n, next_count) for n in curr_node.nodes]
            nodes.extend(next_nodes)
            longest = max(longest, counter)
        else:
            longest = max(longest, counter + 1)

    return longest

def max_num_of_road(n, a, b):
    longest = 0
    nodes = {}

    # 1. Build graph
    i = 0
    while i < len(a):
        node_num_a = a[i]
        node_num_b = b[i]

        node_a = None
        if node_num_a in nodes:
            node_a = nodes[node_num_a]
        else:
            node_a = Node(node_num_a)
            nodes[node_num_a] = node_a
        
        node_b = None
        if node_num_b in nodes:
            node_b = nodes[node_num_b]
        else:
            node_b = Node(node_num_b)
            nodes[node_num_b] = node_b
        
        node_a.connect(node_b)

        i = i + 1

    # 2. Find max road
    for key in nodes.keys():
        edge_count = count_max_edge(nodes[key])
        longest = max(longest, edge_count)

    return longest

for case in TEST_CASES:
    result = False
    _input = case['input']
    n = _input['N']
    a = _input['A']
    b = _input['B']

    expected = case['expected']
    try:
        actual = max_num_of_road(n, a, b)

        result = expected == actual

        print(_input, ' -> ', expected, ' == ', actual, ' = ', result)
    except Exception as e:
        print(_input, ' -> ', 'Error: ', e)
