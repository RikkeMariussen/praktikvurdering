import { useEffect, useState } from "react";
import ReportSelector from "./components/ReportSelector";
import NoteEditor from "./components/NoteEditor";
import EvaluationView from "./components/EvaluationView";

interface Result {
  content: string;
  filename: string;
}

export default function App() {
  const [reports, setReports] = useState<string[]>([]);
  const [selected, setSelected] = useState("");
  const [notes, setNotes] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState<Result | null>(null);

  useEffect(() => {
    fetch("/reports")
      .then((r) => r.json())
      .then((data) => setReports(data.reports))
      .catch(() => setError("Kunne ikke hente rapportliste fra backend."));
  }, []);

  async function runEvaluation() {
    if (!selected) return;
    setLoading(true);
    setError("");
    setResult(null);

    try {
      const res = await fetch("/evaluate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ filename: selected, notes }),
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.detail ?? "Ukendt fejl");
      }

      const data = await res.json();
      setResult({ content: data.result, filename: data.output_file });
    } catch (e) {
      setError(e instanceof Error ? e.message : "Noget gik galt.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <h1>Praktikrapport-vurdering</h1>

        <ReportSelector
          reports={reports}
          selected={selected}
          onChange={setSelected}
          disabled={loading}
        />

        <NoteEditor
          value={notes}
          onChange={setNotes}
          disabled={loading}
        />

        <button
          className="run-btn"
          onClick={runEvaluation}
          disabled={!selected || loading}
        >
          {loading ? "Vurderer…" : "Kør vurdering"}
        </button>

        {error && <p className="error-msg">{error}</p>}
      </aside>

      <main className="main">
        {loading && (
          <div className="loading">
            <div className="spinner" />
            Kalder OpenAI, vent venligst…
          </div>
        )}

        {!loading && result && (
          <EvaluationView content={result.content} filename={result.filename} />
        )}

        {!loading && !result && (
          <div className="placeholder">
            Vælg en rapport og tryk "Kør vurdering"
          </div>
        )}
      </main>
    </div>
  );
}
