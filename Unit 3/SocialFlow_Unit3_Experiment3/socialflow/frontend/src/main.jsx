import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import { Provider, useDispatch, useSelector } from "react-redux";
import {
  LayoutDashboard,
  CalendarDays,
  PenLine,
  BarChart3,
  Users,
  Send,
  LogOut,
  ArrowUpRight,
  Plus,
  ChevronLeft,
  ChevronRight,
  Check,
  Clock,
  MessageSquare,
  FileText,
  RefreshCw,
  Search,
  BookOpen,
  Radio,
  MoreHorizontal,
} from "lucide-react";
import { store, api, setToken, refresh, clear } from "./store";
import "./style.css";
const date = (t) =>
  t
    ? new Date(t).toLocaleString([], {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      })
    : "Not scheduled";
const badge = (s) => (
  <span className={"badge " + s.toLowerCase()}>{s.toLowerCase()}</span>
);
const nav = [
  ["Overview", LayoutDashboard],
  ["Content studio", PenLine],
  ["Calendar", CalendarDays],
  ["Analytics", BarChart3],
  ["Team", Users],
];
export default function App() {
  const dispatch = useDispatch(),
    data = useSelector((s) => s.workspace);
  const [user, setUser] = useState(null),
    [page, setPage] = useState("Overview"),
    [notice, setNotice] = useState(""),
    [busy, setBusy] = useState(false),
    [selected, setSelected] = useState(null),
    [filter, setFilter] = useState("ALL"),
    [query, setQuery] = useState(""),
    [members, setMembers] = useState([]),
    [month, setMonth] = useState(new Date()),
    [comments, setComments] = useState([]),
    [comment, setComment] = useState("");
  const [draft, setDraft] = useState({
      title: "",
      content: "",
      channel: "Demo LinkedIn",
    }),
    [when, setWhen] = useState(""),
    [failure, setFailure] = useState(false),
    [editId, setEditId] = useState(null);
  const logout = () => {
    setToken("");
    setUser(null);
    dispatch(clear());
    setSelected(null);
    setPage("Overview");
  };
  useEffect(() => {
    const expired = () => {
      logout();
      setNotice("Your session expired. Please sign in again.");
    };
    window.addEventListener("session-expired", expired);
    return () => window.removeEventListener("session-expired", expired);
  }, []);
  useEffect(() => {
    if (!user) return;
    dispatch(refresh());
    const id = setInterval(() => dispatch(refresh()), 4000);
    return () => clearInterval(id);
  }, [user]);
  useEffect(() => {
    if (user && page === "Team")
      api("/members")
        .then(setMembers)
        .catch((e) => setNotice(e.message));
  }, [page, user]);
  useEffect(() => {
    if (selected)
      api("/posts/" + selected.id + "/comments")
        .then(setComments)
        .catch((e) => setNotice(e.message));
  }, [selected]);
  const writable = user && user.role !== "VIEWER";
  const canEdit = (p) =>
    writable && (user.role === "ADMIN" || p.owner_id === user.id);
  async function act(fn, message) {
    setBusy(true);
    setNotice("");
    try {
      await fn();
      dispatch(refresh());
      setNotice(message);
    } catch (e) {
      setNotice(e.message);
    } finally {
      setBusy(false);
    }
  }
  function compose(p) {
    setEditId(p?.id || null);
    setDraft(
      p
        ? { title: p.title, content: p.content, channel: p.channel }
        : { title: "", content: "", channel: "Demo LinkedIn" },
    );
    setWhen("");
    setFailure(false);
    setSelected(null);
    setPage("Content studio");
  }
  async function save(mode) {
    await act(
      async () => {
        if (!draft.title.trim() || !draft.content.trim())
          throw new Error("Add a title and some content first.");
        let due = 0;
        if (mode === "schedule") {
          due = new Date(when).getTime();
          if (!Number.isFinite(due) || due <= Date.now())
            throw new Error("Choose a future date and time.");
        }
        const r = await api(
          "/posts" + (editId ? "/" + editId : ""),
          editId ? "PUT" : "POST",
          draft,
        );
        setEditId(r.id);
        if (mode !== "draft")
          await api("/posts/" + r.id + "/schedule", "POST", {
            dueAt: due,
            failDemo: failure,
          });
        setDraft({ title: "", content: "", channel: "Demo LinkedIn" });
        setEditId(null);
        setWhen("");
        setPage("Overview");
      },
      mode === "draft"
        ? "Draft saved."
        : mode === "schedule"
          ? "Post scheduled. Kafka will publish it when due."
          : "Post queued for publishing.",
    );
  }
  if (!user)
    return (
      <div className="login">
        <section className="login-art">
          <div className="brand">
            <span className="logo">S</span>SocialFlow
          </div>
          <div>
            <p className="eyebrow">LESS JUGGLING. MORE CREATING.</p>
            <h1>
              Good ideas deserve
              <br />a great workflow.
            </h1>
            <p>
              One shared space to create, collaborate,
              <br />
              and keep your publishing moving.
            </p>
            <div className="art-card">
              <span className="mini-icon">
                <Send />
              </span>
              <b>Your next great post</b>
              <span className="badge published">Ready to flow</span>
              <div className="art-line" />
              <div className="art-line short" />
              <div className="avatars">
                <i>A</i>
                <i>M</i>
                <i>You</i>
                <span>Made better, together.</span>
              </div>
            </div>
          </div>
          <small>UNIT 3 · FULL-STACK CAPSTONE</small>
        </section>
        <section className="login-form">
          <div className="login-box">
            <p className="eyebrow">YOUR PUBLISHING WORKSPACE</p>
            <h2>Welcome back.</h2>
            <p className="muted">
              Sign in and pick up where your ideas left off.
            </p>
            {notice && (
              <div role="alert" className="notice">
                {notice}
              </div>
            )}
            <form
              onSubmit={(e) => {
                e.preventDefault();
                const f = new FormData(e.currentTarget);
                act(async () => {
                  const r = await api("/login", "POST", Object.fromEntries(f));
                  setToken(r.token);
                  setUser(r.user);
                }, "");
              }}
            >
              <label>
                Email address
                <input
                  name="email"
                  type="email"
                  required
                  defaultValue="admin@socialflow.local"
                  autoComplete="username"
                />
              </label>
              <label>
                Password
                <input
                  name="password"
                  type="password"
                  required
                  autoComplete="current-password"
                />
              </label>
              <button className="primary wide" disabled={busy}>
                Sign in <ArrowUpRight size={18} />
              </button>
            </form>
            <div className="demo-note">
              <b>Classroom demo accounts</b>
              <p>
                admin@socialflow.local · Admin@12345
                <br />
                editor@socialflow.local · Editor@12345
                <br />
                viewer@socialflow.local · Viewer@12345
              </p>
              <small>Available only when demo mode is enabled.</small>
            </div>
          </div>
        </section>
      </div>
    );
  const counts = Object.fromEntries(
    data.counts.map((c) => [c.status, Number(c.total)]),
  );
  const posts = data.posts.filter(
    (p) =>
      (filter === "ALL" || p.status === filter) &&
      [p.title, p.content, p.author]
        .join(" ")
        .toLowerCase()
        .includes(query.toLowerCase()),
  );
  const total = Object.values(counts).reduce((a, b) => a + b, 0);
  const pub = counts.PUBLISHED || 0;
  const activePost =
    (selected && data.posts.find((p) => p.id === selected.id)) || selected;
  function postRows(list) {
    return list.length ? (
      <div className="post-list">
        {list.map((p) => (
          <button
            className="post-row"
            key={p.id}
            onClick={() => setSelected(p)}
          >
            <span
              className={
                "channel-icon " +
                (p.channel.includes("Instagram") ? "pink" : "")
              }
            >
              {p.channel.includes("LinkedIn")
                ? "in"
                : p.channel.includes("Instagram")
                  ? "◎"
                  : "𝕏"}
            </span>
            <span className="post-title">
              <b>{p.title}</b>
              <small>
                {p.author} · {p.channel}
              </small>
            </span>
            {badge(p.status)}
            <span className="post-date">
              {date(p.published_at || p.due_at || p.created_at)}
            </span>
            <ChevronRight size={17} />
          </button>
        ))}
      </div>
    ) : (
      <div className="empty">
        <FileText />
        <h3>A little space for your next big idea.</h3>
        <p>Create your first post to start filling this workspace.</p>
        {writable && (
          <button className="secondary" onClick={() => compose()}>
            Create a post
          </button>
        )}
      </div>
    );
  }
  return (
    <div className="shell">
      <aside>
        <div className="brand">
          <span className="logo">S</span>SocialFlow
          <span className="brand-dot" />
        </div>
        <div className="workspace-label">
          <span className="workspace-icon">ST</span>
          <div>
            <b>Studio workspace</b>
            <small>Classroom edition</small>
          </div>
        </div>
        <p className="nav-label">WORKSPACE</p>
        <nav>
          {nav.map(([n, Icon]) => (
            <button
              key={n}
              className={page === n ? "active" : ""}
              onClick={() => {
                if (n === "Content studio" && !writable) return;
                setPage(n);
              }}
              disabled={n === "Content studio" && !writable}
            >
              <Icon size={19} />
              {n}
              {n === "Calendar" && counts.SCHEDULED > 0 && (
                <em>{counts.SCHEDULED}</em>
              )}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="demo-panel">
            <Radio size={18} />
            <b>Demo publishing</b>
            <p>
              Real workflows. A local feed.
              <br />
              No external social posts.
            </p>
          </div>
          <a href="/swagger-ui.html" target="_blank" rel="noreferrer">
            <BookOpen size={17} /> API documentation <ArrowUpRight size={15} />
          </a>
          <button className="profile" onClick={logout}>
            <span className="avatar">{user.name.slice(0, 1)}</span>
            <span>
              <b>{user.name}</b>
              <small>{user.role.toLowerCase()}</small>
            </span>
            <LogOut size={17} />
          </button>
        </div>
      </aside>
      <main>
        <header>
          <span>
            Workspace <span className="slash">/</span> <b>{page}</b>
          </span>
          <div className="header-right">
            <span className="live-dot" /> Live workspace{" "}
            <span className="header-avatar">{user.name.slice(0, 1)}</span>
          </div>
        </header>
        <div className="content">
          <div className="page-heading">
            <div>
              <p className="eyebrow">MAKE EVERY POST COUNT</p>
              <h1>
                {page === "Overview"
                  ? `Your ideas, in motion.`
                  : page === "Content studio"
                    ? "Create something worth sharing."
                    : page === "Calendar"
                      ? "A little planning. A lot of possibility."
                      : page === "Analytics"
                        ? "See how your workflow flows."
                        : "Better content starts with a team."}
              </h1>
              <p className="muted">
                {page === "Overview"
                  ? `Welcome back, ${user.name.split(" ")[0]}. Here’s what’s happening in your studio.`
                  : page === "Content studio"
                    ? "From the first draft to the right moment."
                    : page === "Calendar"
                      ? "Your scheduled and published posts, all in one view."
                      : page === "Analytics"
                        ? "Live publishing metrics from your workspace."
                        : "Create together, with the right access for everyone."}
              </p>
            </div>
            {writable && page !== "Content studio" && (
              <button className="primary" onClick={() => compose()}>
                <Plus size={18} /> Create post
              </button>
            )}
          </div>
          {(notice || data.error) && (
            <div role="status" className="notice">
              {notice || data.error}
              <button
                aria-label="Dismiss notification"
                onClick={() => setNotice("")}
              >
                ×
              </button>
            </div>
          )}
          {page === "Overview" && (
            <>
              <section className="stats">
                {[
                  ["Total posts", total, FileText, "Ideas in your workspace"],
                  [
                    "Scheduled",
                    counts.SCHEDULED || 0,
                    Clock,
                    "Good things on the way",
                  ],
                  ["Published", pub, Send, "Delivered to the demo feed"],
                  [
                    "Needs attention",
                    counts.FAILED || 0,
                    RefreshCw,
                    "Failed posts ready to retry",
                  ],
                ].map(([label, value, Icon, sub]) => (
                  <article key={label}>
                    <div>
                      <span>{label}</span>
                      <Icon size={19} />
                    </div>
                    <strong>{value.toString().padStart(2, "0")}</strong>
                    <small>{sub}</small>
                  </article>
                ))}
              </section>
              <div className="overview-grid">
                <section className="panel">
                  <div className="panel-head">
                    <h2>Your content pipeline</h2>
                    <span className="pill">{total} posts</span>
                  </div>
                  <div className="tools">
                    <div className="tabs">
                      {["ALL", "DRAFT", "SCHEDULED", "PUBLISHED", "FAILED"].map(
                        (s) => (
                          <button
                            key={s}
                            className={filter === s ? "chosen" : ""}
                            onClick={() => setFilter(s)}
                          >
                            {s === "ALL" ? "All posts" : s.toLowerCase()}
                          </button>
                        ),
                      )}
                    </div>
                    <label className="search">
                      <Search size={16} />
                      <input
                        placeholder="Search posts"
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                      />
                    </label>
                  </div>
                  {postRows(posts)}
                </section>
                <div className="right-col">
                  <section className="inspiration">
                    <span className="pill">THE CREATIVE CORNER</span>
                    <h2>
                      A small idea today.
                      <br />A great post tomorrow.
                    </h2>
                    <p>
                      Keep your drafts close and your next publishing moment
                      closer.
                    </p>
                    {writable && (
                      <button onClick={() => compose()}>
                        Start a draft <ArrowUpRight size={18} />
                      </button>
                    )}
                    <div className="decor-circle" />
                  </section>
                  <section className="panel activity">
                    <h2>Workspace activity</h2>
                    {data.activity.slice(0, 6).map((a) => (
                      <div className="activity-row" key={a.id}>
                        <span className="activity-dot" />
                        <div>
                          <p>{a.message}</p>
                          <small>{date(a.created_at)}</small>
                        </div>
                      </div>
                    ))}
                  </section>
                </div>
              </div>
            </>
          )}
          {page === "Content studio" && writable && (
            <div className="composer-grid">
              <section className="panel composer">
                <div className="panel-head">
                  <h2>{editId ? "Edit draft" : "New post"}</h2>
                  <span className="pill">Draft studio</span>
                </div>
                <label>
                  Internal title
                  <input
                    value={draft.title}
                    maxLength={120}
                    onChange={(e) =>
                      setDraft({ ...draft, title: e.target.value })
                    }
                    placeholder="Give your idea a name"
                  />
                </label>
                <label>
                  Publishing channel
                  <select
                    value={draft.channel}
                    onChange={(e) =>
                      setDraft({ ...draft, channel: e.target.value })
                    }
                  >
                    {["Demo LinkedIn", "Demo Instagram", "Demo X"].map((c) => (
                      <option key={c}>{c}</option>
                    ))}
                  </select>
                </label>
                <label>
                  Your content
                  <textarea
                    rows={9}
                    value={draft.content}
                    maxLength={2800}
                    onChange={(e) =>
                      setDraft({ ...draft, content: e.target.value })
                    }
                    placeholder="What would you like to share with the world?"
                  />
                </label>
                <p className="char-count">
                  {draft.content.length} / 2800 characters
                </p>
                <label>
                  Schedule for later{" "}
                  <span className="muted">(your local time)</span>
                  <input
                    type="datetime-local"
                    value={when}
                    onChange={(e) => setWhen(e.target.value)}
                  />
                </label>
                {user.role === "ADMIN" && (
                  <label className="check-label">
                    <input
                      type="checkbox"
                      checked={failure}
                      onChange={(e) => setFailure(e.target.checked)}
                    />
                    Test a publishing failure (retries → failed queue)
                  </label>
                )}
                <div className="form-actions">
                  <button
                    disabled={busy}
                    className="secondary"
                    onClick={() => save("draft")}
                  >
                    Save draft
                  </button>
                  <button
                    disabled={busy || !when}
                    className="secondary"
                    onClick={() => save("schedule")}
                  >
                    <CalendarDays size={16} /> Schedule
                  </button>
                  <button
                    disabled={busy}
                    className="primary"
                    onClick={() => save("now")}
                  >
                    <Send size={16} /> Publish now
                  </button>
                </div>
              </section>
              <section>
                <div className="preview-label">
                  <b>POST PREVIEW</b>
                  <span>{draft.channel}</span>
                </div>
                <div className="panel preview">
                  <div className="preview-author">
                    <span className="avatar">{user.name[0]}</span>
                    <div>
                      <b>{user.name}</b>
                      <small>Studio workspace · Demo channel</small>
                    </div>
                    <MoreHorizontal />
                  </div>
                  <p>
                    {draft.content ||
                      "Your words will appear here. Make them count."}
                  </p>
                  <div className="preview-bottom">
                    Demo preview · No external social account connected
                  </div>
                </div>
                <div className="tip">
                  <b>A thoughtful workflow</b>
                  <p>
                    Save a draft, invite feedback in the post’s discussion, then
                    choose the perfect publishing time.
                  </p>
                </div>
              </section>
            </div>
          )}
          {page === "Calendar" && (
            <section className="panel calendar-panel">
              <div className="panel-head">
                <h2>
                  {month.toLocaleDateString([], {
                    month: "long",
                    year: "numeric",
                  })}
                </h2>
                <div className="calendar-nav">
                  <button
                    aria-label="Previous month"
                    onClick={() =>
                      setMonth(
                        new Date(month.getFullYear(), month.getMonth() - 1, 1),
                      )
                    }
                  >
                    <ChevronLeft />
                  </button>
                  <button onClick={() => setMonth(new Date())}>Today</button>
                  <button
                    aria-label="Next month"
                    onClick={() =>
                      setMonth(
                        new Date(month.getFullYear(), month.getMonth() + 1, 1),
                      )
                    }
                  >
                    <ChevronRight />
                  </button>
                </div>
              </div>
              <div className="calendar">
                {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((d) => (
                  <div className="day-name" key={d}>
                    {d}
                  </div>
                ))}
                {Array.from({ length: 42 }, (_, i) => {
                  const d = new Date(
                    month.getFullYear(),
                    month.getMonth(),
                    1 -
                      new Date(
                        month.getFullYear(),
                        month.getMonth(),
                        1,
                      ).getDay() +
                      i,
                  );
                  const dayPosts = data.posts.filter(
                    (p) =>
                      p.due_at &&
                      new Date(p.published_at || p.due_at).toDateString() ===
                        d.toDateString(),
                  );
                  return (
                    <div
                      className={
                        "day " +
                        (d.getMonth() !== month.getMonth() ? "outside " : "") +
                        (d.toDateString() === new Date().toDateString()
                          ? "today"
                          : "")
                      }
                      key={i}
                    >
                      <span>{d.getDate()}</span>
                      {dayPosts.map((p) => (
                        <button
                          key={p.id}
                          className={"calendar-post " + p.status.toLowerCase()}
                          onClick={() => setSelected(p)}
                        >
                          {p.title}
                        </button>
                      ))}
                    </div>
                  );
                })}
              </div>
              <p className="calendar-foot">
                Dates use your browser’s timezone. Drafts appear once scheduled.
              </p>
            </section>
          )}
          {page === "Analytics" && (
            <>
              <section className="stats">
                {[
                  ["Published posts", pub],
                  ["Scheduled posts", counts.SCHEDULED || 0],
                  ["Failed posts", counts.FAILED || 0],
                  [
                    "Publishing success",
                    `${pub + (counts.FAILED || 0) ? Math.round((pub / (pub + (counts.FAILED || 0))) * 100) : 0}%`,
                  ],
                ].map(([label, n]) => (
                  <article key={label}>
                    <span>{label}</span>
                    <strong>{n}</strong>
                    <small>Actual workspace records</small>
                  </article>
                ))}
              </section>
              <div className="analytics-grid">
                <section className="panel">
                  <div className="panel-head">
                    <h2>Publishing rhythm</h2>
                    <span className="pill">Last 7 days</span>
                  </div>
                  <div className="chart">
                    {Array.from({ length: 7 }, (_, i) => {
                      let d = new Date();
                      d.setDate(d.getDate() - 6 + i);
                      let n = data.daily.filter(
                        (p) =>
                          new Date(p.published_at).toDateString() ===
                          d.toDateString(),
                      ).length;
                      return (
                        <div className="bar-column" key={i}>
                          <b>{n}</b>
                          <div
                            className="bar"
                            style={{ height: Math.min(160, 8 + n * 20) + "px" }}
                          />
                          <span>
                            {d.toLocaleDateString([], { weekday: "short" })}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </section>
                <section className="panel">
                  <div className="panel-head">
                    <h2>Channel breakdown</h2>
                  </div>
                  {["Demo LinkedIn", "Demo Instagram", "Demo X"].map((c) => (
                    <div className="channel-summary" key={c}>
                      <span>{c}</span>
                      <strong>
                        {
                          data.posts.filter(
                            (p) => p.channel === c && p.status === "PUBLISHED",
                          ).length
                        }
                      </strong>
                    </div>
                  ))}
                  <p className="small-note">
                    Counts by channel use the latest 500 posts shown in the
                    dashboard.
                  </p>
                </section>
              </div>
              <div className="notice">
                These are publishing analytics. Likes, impressions, and
                engagement need a real provider integration and are not
                simulated.
              </div>
              <section className="panel">
                <div className="panel-head">
                  <h2>Demo publication feed</h2>
                </div>
                {postRows(data.posts.filter((p) => p.status === "PUBLISHED"))}
              </section>
            </>
          )}
          {page === "Team" && (
            <div className="team-grid">
              <section className="panel">
                <div className="panel-head">
                  <h2>The people behind the posts</h2>
                  <span className="pill">{members.length} members</span>
                </div>
                {members.map((m) => (
                  <div className="member" key={m.id}>
                    <span className="avatar">{m.name[0]}</span>
                    <div>
                      <b>{m.name}</b>
                      <small>{m.email}</small>
                    </div>
                    <span className="pill">{m.role}</span>
                    <span
                      className={"badge " + (m.active ? "published" : "failed")}
                    >
                      {m.active ? "active" : "disabled"}
                    </span>
                    {user.role === "ADMIN" && m.role !== "ADMIN" && (
                      <button
                        className="text-button"
                        disabled={busy}
                        onClick={() =>
                          act(async () => {
                            await api("/members/" + m.id + "/toggle", "POST");
                            setMembers(await api("/members"));
                          }, "Member access updated.")
                        }
                      >
                        {m.active ? "Disable" : "Enable"}
                      </button>
                    )}
                  </div>
                ))}
              </section>
              <section className="panel add-member">
                <h2>
                  {user.role === "ADMIN" ? "Grow your team" : "Workspace roles"}
                </h2>
                {user.role === "ADMIN" ? (
                  <form
                    onSubmit={(e) => {
                      e.preventDefault();
                      const f = e.currentTarget,
                        body = Object.fromEntries(new FormData(f));
                      act(async () => {
                        await api("/members", "POST", body);
                        setMembers(await api("/members"));
                        f.reset();
                      }, "Team member created. Share their credentials securely.");
                    }}
                  >
                    <label>
                      Full name
                      <input name="name" required maxLength={80} />
                    </label>
                    <label>
                      Email address
                      <input
                        name="email"
                        type="email"
                        required
                        maxLength={160}
                      />
                    </label>
                    <label>
                      Initial password
                      <input
                        name="password"
                        type="password"
                        minLength={10}
                        maxLength={72}
                        required
                      />
                    </label>
                    <label>
                      Role
                      <select name="role">
                        <option>EDITOR</option>
                        <option>VIEWER</option>
                      </select>
                    </label>
                    <button className="primary wide" disabled={busy}>
                      <Plus size={16} /> Add member
                    </button>
                  </form>
                ) : (
                  <p className="muted">
                    Admins manage the team and all posts. Editors create and
                    manage their own posts, and discuss any post. Viewers can
                    read content, calendar, and analytics.
                  </p>
                )}
              </section>
            </div>
          )}
          <footer>
            SocialFlow <span>·</span> Create with intention. Publish with
            confidence.{" "}
            <span className="footer-mode">Classroom demo · JWT protected</span>
          </footer>
        </div>
      </main>
      {activePost && (
        <div className="modal-backdrop" onClick={() => setSelected(null)}>
          <section
            role="dialog"
            aria-modal="true"
            aria-label="Post details"
            className="modal"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              className="modal-close"
              aria-label="Close post"
              onClick={() => setSelected(null)}
            >
              ×
            </button>
            <p className="eyebrow">{activePost.channel}</p>
            <h2>{activePost.title}</h2>
            {badge(activePost.status)}
            <p className="post-body">{activePost.content}</p>
            <p className="muted">
              By {activePost.author} ·{" "}
              {date(
                activePost.published_at ||
                  activePost.due_at ||
                  activePost.created_at,
              )}
            </p>
            {canEdit(activePost) && (
              <div className="form-actions">
                {activePost.status === "DRAFT" && (
                  <>
                    <button
                      className="secondary"
                      onClick={() => compose(activePost)}
                    >
                      Edit draft
                    </button>
                    <button
                      className="primary"
                      disabled={busy}
                      onClick={() =>
                        act(
                          () =>
                            api(
                              "/posts/" + activePost.id + "/schedule",
                              "POST",
                              { dueAt: 0, failDemo: false },
                            ),
                          "Post queued.",
                        )
                      }
                    >
                      Publish now
                    </button>
                  </>
                )}
                {activePost.status === "SCHEDULED" && (
                  <button
                    disabled={busy}
                    className="secondary"
                    onClick={() =>
                      act(
                        () =>
                          api("/posts/" + activePost.id + "/cancel", "POST"),
                        "Schedule cancelled. Post returned to drafts.",
                      )
                    }
                  >
                    Cancel schedule
                  </button>
                )}
                {activePost.status === "FAILED" && (
                  <button
                    disabled={busy}
                    className="primary"
                    onClick={() =>
                      act(
                        () =>
                          api("/posts/" + activePost.id + "/schedule", "POST", {
                            dueAt: 0,
                            failDemo: false,
                          }),
                        "Post queued again with failure injection disabled.",
                      )
                    }
                  >
                    Retry publishing
                  </button>
                )}
              </div>
            )}
            <div className="discussion">
              <h3>
                <MessageSquare size={17} /> Team discussion
              </h3>
              {comments.length ? (
                comments.map((c) => (
                  <div className="comment" key={c.id}>
                    <b>{c.author}</b>
                    <small>{date(c.created_at)}</small>
                    <p>{c.body}</p>
                  </div>
                ))
              ) : (
                <p className="muted">
                  No comments yet. Start the conversation.
                </p>
              )}
              {writable && (
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    act(async () => {
                      await api(
                        "/posts/" + activePost.id + "/comments",
                        "POST",
                        { body: comment },
                      );
                      setComments(
                        await api("/posts/" + activePost.id + "/comments"),
                      );
                      setComment("");
                    }, "Comment added.");
                  }}
                >
                  <label>
                    Add a comment
                    <textarea
                      required
                      maxLength={800}
                      value={comment}
                      onChange={(e) => setComment(e.target.value)}
                    />
                  </label>
                  <button className="secondary" disabled={busy}>
                    Send comment
                  </button>
                </form>
              )}
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
