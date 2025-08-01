    # Start from the official RabbitMQ image
    FROM rabbitmq:3.12-management

    # Set the Erlang cookie as a build argument
    ARG ERLANG_COOKIE="SUPERSECRETRABBITMQCOOKIE"

    # Create the .erlang.cookie file with the specified content and correct permissions
    # This ensures the file is created with the right ownership and permissions
    # directly inside the image, bypassing host volume issues for the cookie.
    RUN echo "$ERLANG_COOKIE" > /var/lib/rabbitmq/.erlang.cookie && \
        chmod 400 /var/lib/rabbitmq/.erlang.cookie && \
        chown rabbitmq:rabbitmq /var/lib/rabbitmq/.erlang.cookie

    # The rest of the RabbitMQ setup is handled by the base image.
    # No need to expose ports or define entrypoint here, as the base image does it.

    LABEL maintainer="Your Name <your.email@example.com>"
    LABEL version="0.0.1"
    LABEL description="Custom RabbitMQ image with pre-configured Erlang cookie"
    