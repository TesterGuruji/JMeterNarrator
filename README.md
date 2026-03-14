## JMeter Narrator

**Purpose**: A custom JMeter listener that aggregates test results and calls Google's Gemini LLM to generate human-readable performance observations, summaries, and recommendations from `.jtl` data.

### Features

- **Live aggregation** of response times, error rates, throughput, and per-label stats while the test runs.
- **One-click LLM analysis** button in the listener GUI.
- Uses **Gemini** (default `gemini-2.5-flash`) for performance analysis.
- API key is read from environment or JMeter properties (not stored in the test plan).

### Build

From the project root:

```bash
mvn clean package
```

The plugin JAR will be created in the `target` directory (for example, `target/jmeter-llm-gemini-listener-0.1.0-SNAPSHOT.jar`).

### Install into JMeter

1. Build the project as above.
2. Copy the generated JAR from `target/` to your JMeter `lib/ext` directory.
3. Restart JMeter.
4. In your Test Plan, add:
   - `Add` → `Listener` → `JMeter Narrator`.

### Configure Gemini

Set your Gemini API key by either:

- Environment variable:

  ```bash
  export GEMINI_API_KEY=your_api_key_here
  ```

- Or in JMeter `user.properties`:

  ```properties
  gemini.api.key=your_api_key_here
  ```

Optional: configure a default model in `user.properties`:

```properties
gemini.model=gemini-1.5-flash
```

### Usage

1. Add the **JMeter Narrator** listener to your test plan.
2. Run your test as usual; the listener will aggregate metrics.
3. After or during the test, click **Run Analysis Now** in the listener GUI.
4. The Gemini-generated Markdown analysis will appear in the output area.

