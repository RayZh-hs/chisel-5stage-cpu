# Stall

In this implementation stalls occur in the following circumstances:

1. When a branching instruction is fetched (IF), the fetcher will halt until the value is returned and pc overwritten
2. When hazards are detected (ID), the fetcher and decoder will halt until the hazard passes
