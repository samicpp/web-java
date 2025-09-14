if "count" not in shared:
    shared["count"]=0
    pass

socket.close(f"count is at {shared['count']}")
shared["count"]+=1
