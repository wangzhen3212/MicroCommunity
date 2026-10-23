FROM registry.cn-qingdao.aliyuncs.com/homecommunity/ubuntu-java17:1.0

MAINTAINER xxx <xxx@xx.com>

# 设置时区为东八区（Asia/Shanghai）
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV LANGUAGE=C.UTF-8
ENV JAVA_OPTS="-Dfile.encoding=UTF-8"

ENV MIN_MEM=-Xms256m
ENV MAX_MEM=-Xmx512m
ENV ACTIVE=dev

COPY springboot/target/MicroCommunityBoot.jar /root/MicroCommunityBoot.jar

RUN echo '#!/bin/bash\njava $JAVA_OPTS -Dspring.profiles.active=$ACTIVE $MIN_MEM $MAX_MEM -jar /root/MicroCommunityBoot.jar' > /root/start_hc_jar.sh \
    && chmod u+x /root/start_hc_jar.sh

EXPOSE 8008

CMD ["/root/start_hc_jar.sh"]
