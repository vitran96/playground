class Solution(object):
    def binaryGap(self, n):
        """
        :type n: int
        :rtype: int
        """
        # This can be optimized with bitwise function
        arr = list(bin(n))[2:]

        gap = 0
        # Simplify the algo by checking last '1' only
        last = -1
        i = 0
        while i < len(arr):
            if arr[i] == '1':
                if last >= 0:
                    gap = max(gap, i - last)
                last = i

            i = i + 1

        return gap