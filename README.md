<img width="652" height="446" alt="Screenshot 2026-03-18 at 8 48 06 PM" src="https://github.com/user-attachments/assets/73470b37-e417-4efd-8dde-5f6ff938683f" />## JMeter Narrator

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




<img width="1503" height="814" alt="Screenshot 2026-03-18 at 8 50 06 PM" src="https://github.com/user-attachments/assets/4d1a4814-9fff-4525-8935-a267eea90b95" />
<img width="1493" height="772" alt="Screenshot 2026-03-18 at 8 49 42 PM" src="https://github.com/user-attachments/assets/1d9e3ff3-b1fa-4aed-995e-98ef1d529178" />
<img width="1195" height="733" alt="Screenshot 2026-03-18 at 8 48 57 PM" src="https://github.com/user-attachments/assets/6a521e86-00b5-46b8-8df6-1ae172366d26" />
<img width="1189" height="433" alt="Screenshot 2026-03-18 at 8 48 23 PM" src="https://github.com/user-attachments/assets/84be79ad-2694-4321-95f4-68009fe4d764" />
<img width="652" height="446" alt="Screenshot 2026-03-18 at 8 48 06 PM" src="https://github.com/user-attachments/assets/ab72be61-dedc-4613-96fc-a165ec5dd090" />
