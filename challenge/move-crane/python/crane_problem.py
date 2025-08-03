import queue


class CraneInput:
    def __init__(
        self,
        a: list[int],
        b: list[int],
        C: int,
        D: int,
        expectation: int,
    ):
        self.a = a
        self.b = b
        self.C = C
        self.D = D
        self.expectation = expectation

    def __repr__(self):
        return f"CraneInput(a={self.a}, b={self.b}, C={self.C}, D={self.D})"


TEST_CASES = [
    CraneInput([1, 4, 7], [1, 2, 2], 0, 9, 2),
    CraneInput([2], [3], 4, 4, 0),
    CraneInput([0, 5], [1, 1], 0, 9, -1),
    CraneInput([1, 2, 3, 4], [1, 1, 1, 1], 1, 5, 1),
    CraneInput([5], [10], 0, 9, 0),
    CraneInput([0, 3, 6], [1, 1, 1], 3, 8, -1),
    CraneInput([0, 3, 6, 9], [1, 1, 1, 2], 3, 10, -1),
    CraneInput([1, 4, 8], [10, 1, 1], 2, 5, 0),
    CraneInput([0, 2, 4, 7], [1, 1, 1, 1], 0, 8, -1),
    CraneInput([1, 2, 3, 8], [2, 2, 2, 3], 2, 11, 1),
]


class Crane:
    def __init__(
        self,
        position: int,
        range: int,
    ):
        self.position = position
        self.left: int = position - range
        self.right: int = position + range
        # self.left_connected_crane: list["Crane"] = []
        # self.right_connected_crane: list["Crane"] = []
        self.connected_cranes: set["Crane"] = set()

    def in_range(self, position: int) -> bool:
        return self.left <= position and self.right >= position

    def add_crane_if_outside_or_connect(self, other: "Crane") -> None:
        if other is self:
            return

        if other.left >= self.left and other.right <= self.right:
            return

        # a.l      b.l   a.r    b.r
        # b.l      a.l   b.r    a.r
        # b.l a.l a.r b.r
        if (other.left <= self.right and other.right > self.right) or \
            (self.left <= other.right and self.right > other.right) or \
            (other.left < self.left and other.right > self.right):
            self.connected_cranes.add(other)

    def __repr__(self):
        return f"({self.position} ({self.left}, {self.right}))"

    # def __eq__(self, other):
    #     if self is other:
    #         return True
    #     if not isinstance(other, "Crane"):
    #         return False

    #     return 


# Assume a and b size > 0
# C <= D
# a is postion
# b is range
def shortest_transit(
    a: list[int],
    b: list[int],
    C: int,
    D: int,
) -> int:
    # NOTE: strategy:
    # create transit graph between all NODE
    # collect all crane that cover C and D
    # then use queue to do multi-source BFS
    if C == D:
        return 0

    # NOTE: create graph
    cranes: list[Crane] = []
    cranes_at_C = queue.Queue()
    cranes_at_D: set[Crane] = set()

    # create crane
    for i in range(0, len(a)):
        p = a[i]
        r = b[i]

        crane = Crane(p, r)
        cranes.append(crane)

        if crane.in_range(C):
            # print(f"crane at C {crane}")
            cranes_at_C.put((crane, 0))

        if crane.in_range(D):
            cranes_at_D.add(crane)

    crane_size = len(cranes)
    # create graph
    for i in range(0, crane_size - 1):
        current = cranes[i]
        for j in range(i, crane_size):
            next = cranes[j]

            next.add_crane_if_outside_or_connect(current)
            current.add_crane_if_outside_or_connect(next)

    # BFS
    visited: set[Crane] = set()

    q = cranes_at_C

    result = -1
    # NOTE: wrong logic
    while not q.empty():
        crane, count = q.get()
        # print(f"queue: {q}")
        # print(crane)
        # print(crane.connected_cranes)
        if crane in cranes_at_D:
            result = count
            # print(f"END: {result}")
            break

        if crane in visited:
            # print("SKIP")
            continue

        visited.add(crane)
        connected_cranes: list[Crane] = crane.connected_cranes
        count = count + 1
        for next_crane in connected_cranes:
            if next_crane in visited:
                continue
            q.put((next_crane, count))

    return result


for case in TEST_CASES:
    try:
        result = shortest_transit(
            case.a,
            case.b,
            case.C,
            case.D,
        )

        if result == case.expectation:
            print("PASS")
        else:
            print(f"FAIL: input ({case}). expectation = {case.expectation} vs actual = {result}")
    except Exception as e:
        print(f"input ({case}). expectation = {case.expectation} vs ERROR: {e}")
