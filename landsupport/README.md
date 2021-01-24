# Notes written by Giuliano Langella | glangella@unina.it

## LOG of compss processes
How to get help and try customize compss instance

```
docker exec -it compss_armosa bash
compss_agent_start -help
```

I could change some configuration in the start_armosa.sh script in order to save the logs of workers in the persistent log folder

`/media/GFTP/landsupport/logs/compss/`

which, in the container, is seen at

`/logs/compss/`


