import React, { useState, useEffect } from 'react';
import { 
  Users, 
  Building2, 
  Calendar, 
  ShieldCheck, 
  LayoutGrid, 
  FileText, 
  Download, 
  RefreshCw, 
  Plus, 
  Trash2, 
  Search, 
  AlertTriangle, 
  CheckCircle2, 
  Sparkles,
  Layers,
  MapPin,
  Clock,
  X
} from 'lucide-react';
import './App.css';

const API_BASE = '/api';

const SUBJECT_COLORS = {
  DBMS: { bg: '#e0f2fe', text: '#0369a1', border: '#bae6fd' },
  JAVA: { bg: '#d1fae5', text: '#047857', border: '#a7f3d0' },
  CN:   { bg: '#f3e8ff', text: '#7e22ce', border: '#e9d5ff' },
  OS:   { bg: '#fef3c7', text: '#b45309', border: '#fde68a' },
  DSA:  { bg: '#ffe4e6', text: '#be123c', border: '#fecdd3' },
};

function getSubjectBadgeStyle(code) {
  return SUBJECT_COLORS[code] || { bg: '#f1f5f9', text: '#475569', border: '#cbd5e1' };
}

export default function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [apiOnline, setApiOnline] = useState(false);
  const [toast, setToast] = useState(null);

  // Core Data
  const [students, setStudents] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [exams, setExams] = useState([]);
  const [invigilators, setInvigilators] = useState([]);
  const [allocationSummary, setAllocationSummary] = useState(null);
  const [structuredAllocations, setStructuredAllocations] = useState(null);
  const [chartText, setChartText] = useState('');
  const [loading, setLoading] = useState(false);

  // Filters & Selected State
  const [selectedDate, setSelectedDate] = useState('2026-12-10');
  const [selectedSlot, setSelectedSlot] = useState('MORNING');
  const [studentSearch, setStudentSearch] = useState('');
  const [selectedSubjectFilter, setSelectedSubjectFilter] = useState('ALL');
  const [seatSearch, setSeatSearch] = useState('');

  // Modals
  const [modalType, setModalType] = useState(null); // 'student', 'room', 'exam', 'invigilator'
  const [studentForm, setStudentForm] = useState({ rollNo: '', name: '', subjectCode: '' });
  const [roomForm, setRoomForm] = useState({ roomName: '', rows: '', cols: '' });
  const [examForm, setExamForm] = useState({ subjectCode: '', examDate: '2026-12-10', slot: 'MORNING' });
  const [invigilatorForm, setInvigilatorForm] = useState({ name: '' });

  // Show Toast
  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 4000);
  };

  // Check Health
  const checkHealth = async () => {
    try {
      const res = await fetch(`${API_BASE}/health`);
      if (res.ok) {
        setApiOnline(true);
      } else {
        setApiOnline(false);
      }
    } catch {
      setApiOnline(false);
    }
  };

  // Fetch All Core Data
  const loadAllData = async () => {
    setLoading(true);
    try {
      await checkHealth();
      const [sRes, rRes, eRes, iRes] = await Promise.all([
        fetch(`${API_BASE}/students`),
        fetch(`${API_BASE}/rooms`),
        fetch(`${API_BASE}/exams`),
        fetch(`${API_BASE}/invigilators`)
      ]);

      if (sRes.ok) setStudents(await sRes.json());
      if (rRes.ok) setRooms(await rRes.json());
      if (eRes.ok) setExams(await eRes.json());
      if (iRes.ok) setInvigilators(await iRes.json());
    } catch (err) {
      console.error(err);
      showToast('Failed to connect to Java API server. Ensure ApiServer is running on port 8080.', 'error');
    } finally {
      setLoading(false);
    }
  };

  // Load Seating Data for active session
  const loadAllocations = async (date = selectedDate, slot = selectedSlot) => {
    try {
      const res = await fetch(`${API_BASE}/allocations?date=${date}&slot=${slot}`);
      if (res.ok) {
        const data = await res.json();
        setStructuredAllocations(data);
      } else {
        setStructuredAllocations(null);
      }

      const chartRes = await fetch(`${API_BASE}/reports/chart?date=${date}&slot=${slot}`);
      if (chartRes.ok) {
        const cData = await chartRes.json();
        setChartText(cData.chart);
      } else {
        setChartText('');
      }
    } catch {
      setStructuredAllocations(null);
      setChartText('');
    }
  };

  useEffect(() => {
    loadAllData();
    const interval = setInterval(checkHealth, 5000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (apiOnline) {
      loadAllocations();
    }
  }, [apiOnline, selectedDate, selectedSlot]);

  // Run Allocation
  const handleRunAllocation = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/allocate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ date: selectedDate, slot: selectedSlot })
      });
      const data = await res.json();
      if (!res.ok) {
        showToast(data.error || 'Allocation failed', 'error');
      } else {
        setAllocationSummary(data);
        showToast(`Success! ${data.seated} students seated across ${data.roomsUsed} rooms.`, 'success');
        await loadAllocations();
        setActiveTab('seating');
      }
    } catch (err) {
      showToast('Failed to execute allocation: ' + err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  // Clear Allocation
  const handleClearAllocation = async () => {
    if (!window.confirm(`Clear seating allocation for ${selectedDate} (${selectedSlot})?`)) return;
    try {
      const res = await fetch(`${API_BASE}/allocate?date=${selectedDate}&slot=${selectedSlot}`, {
        method: 'DELETE'
      });
      if (res.ok) {
        showToast('Session cleared successfully', 'success');
        setAllocationSummary(null);
        setStructuredAllocations(null);
        setChartText('');
      } else {
        const data = await res.json();
        showToast(data.error || 'Failed to clear session', 'error');
      }
    } catch (err) {
      showToast('Error: ' + err.message, 'error');
    }
  };

  // Download CSV
  const handleDownloadCsv = () => {
    window.open(`${API_BASE}/reports/csv?date=${selectedDate}&slot=${selectedSlot}`, '_blank');
  };

  // Add Student
  const handleAddStudent = async (e) => {
    e.preventDefault();
    try {
      const res = await fetch(`${API_BASE}/students`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          rollNo: parseInt(studentForm.rollNo, 10),
          name: studentForm.name,
          subjectCode: studentForm.subjectCode
        })
      });
      const data = await res.json();
      if (!res.ok) {
        showToast(data.error || 'Failed to add student', 'error');
      } else {
        showToast('Student added successfully', 'success');
        setStudentForm({ rollNo: '', name: '', subjectCode: '' });
        setModalType(null);
        loadAllData();
      }
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  // Delete Student
  const handleDeleteStudent = async (rollNo) => {
    if (!window.confirm(`Delete student roll no ${rollNo}?`)) return;
    try {
      const res = await fetch(`${API_BASE}/students?rollNo=${rollNo}`, { method: 'DELETE' });
      if (res.ok) {
        showToast(`Student ${rollNo} deleted`, 'success');
        loadAllData();
      }
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  // Add Room
  const handleAddRoom = async (e) => {
    e.preventDefault();
    try {
      const res = await fetch(`${API_BASE}/rooms`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          roomName: roomForm.roomName,
          rows: parseInt(roomForm.rows, 10),
          cols: parseInt(roomForm.cols, 10)
        })
      });
      const data = await res.json();
      if (!res.ok) {
        showToast(data.error || 'Failed to add room', 'error');
      } else {
        showToast('Room created successfully', 'success');
        setRoomForm({ roomName: '', rows: '', cols: '' });
        setModalType(null);
        loadAllData();
      }
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  // Delete Room
  const handleDeleteRoom = async (roomId, name) => {
    if (!window.confirm(`Delete ${name}?`)) return;
    try {
      const res = await fetch(`${API_BASE}/rooms?roomId=${roomId}`, { method: 'DELETE' });
      if (res.ok) {
        showToast(`${name} deleted`, 'success');
        loadAllData();
      }
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  // Add Exam
  const handleAddExam = async (e) => {
    e.preventDefault();
    try {
      const res = await fetch(`${API_BASE}/exams`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(examForm)
      });
      const data = await res.json();
      if (!res.ok) {
        showToast(data.error || 'Failed to add exam', 'error');
      } else {
        showToast('Exam scheduled successfully', 'success');
        setExamForm({ subjectCode: '', examDate: '2026-12-10', slot: 'MORNING' });
        setModalType(null);
        loadAllData();
      }
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  // Delete Exam
  const handleDeleteExam = async (examId) => {
    if (!window.confirm(`Delete exam #${examId}?`)) return;
    try {
      const res = await fetch(`${API_BASE}/exams?examId=${examId}`, { method: 'DELETE' });
      if (res.ok) {
        showToast(`Exam #${examId} deleted`, 'success');
        loadAllData();
      }
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  // Add Invigilator
  const handleAddInvigilator = async (e) => {
    e.preventDefault();
    try {
      const res = await fetch(`${API_BASE}/invigilators`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(invigilatorForm)
      });
      const data = await res.json();
      if (!res.ok) {
        showToast(data.error || 'Failed to add invigilator', 'error');
      } else {
        showToast('Invigilator added successfully', 'success');
        setInvigilatorForm({ name: '' });
        setModalType(null);
        loadAllData();
      }
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  // Delete Invigilator
  const handleDeleteInvigilator = async (id, name) => {
    if (!window.confirm(`Delete ${name}?`)) return;
    try {
      const res = await fetch(`${API_BASE}/invigilators?invigilatorId=${id}`, { method: 'DELETE' });
      if (res.ok) {
        showToast(`${name} deleted`, 'success');
        loadAllData();
      }
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  // Computed Values
  const totalCapacity = rooms.reduce((sum, r) => sum + r.capacity, 0);
  const subjectsList = Array.from(new Set(students.map(s => s.subjectCode)));

  const filteredStudents = students.filter(s => {
    const matchSearch = s.name.toLowerCase().includes(studentSearch.toLowerCase()) ||
                        String(s.rollNo).includes(studentSearch);
    const matchSubject = selectedSubjectFilter === 'ALL' || s.subjectCode === selectedSubjectFilter;
    return matchSearch && matchSubject;
  });

  return (
    <div className="app-container">
      {/* Toast Messages */}
      {toast && (
        <div className="toast-container">
          <div className={`toast ${toast.type}`}>
            {toast.type === 'success' && <CheckCircle2 size={18} />}
            {toast.type === 'error' && <AlertTriangle size={18} />}
            <span>{toast.message}</span>
          </div>
        </div>
      )}

      {/* Top Header */}
      <header className="app-header">
        <div className="brand-section">
          <div className="brand-logo-badge">व्य</div>
          <div>
            <div className="brand-title">VYAVASTHA (व्यवस्था)</div>
            <div className="brand-subtitle">Intelligent Examination Seating & Invigilation Management</div>
          </div>
        </div>

        <div className="header-right">
          <div className={`status-pill ${apiOnline ? 'online' : 'offline'}`}>
            <span className={`status-dot ${apiOnline ? 'online' : 'offline'}`}></span>
            <span>{apiOnline ? 'Java REST API Online (:8080)' : 'API Disconnected'}</span>
          </div>

          <button className="btn btn-secondary btn-sm" onClick={loadAllData} title="Refresh All Data">
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            <span>Sync</span>
          </button>
        </div>
      </header>

      {/* Main Navigation Tabs */}
      <nav className="nav-bar">
        <button 
          className={`nav-tab ${activeTab === 'dashboard' ? 'active' : ''}`}
          onClick={() => setActiveTab('dashboard')}
        >
          <LayoutGrid size={16} />
          <span>Dashboard</span>
        </button>

        <button 
          className={`nav-tab ${activeTab === 'allocation' ? 'active' : ''}`}
          onClick={() => setActiveTab('allocation')}
        >
          <Sparkles size={16} />
          <span>Allocation Engine</span>
        </button>

        <button 
          className={`nav-tab ${activeTab === 'seating' ? 'active' : ''}`}
          onClick={() => setActiveTab('seating')}
        >
          <Layers size={16} />
          <span>Visual Seating Chart</span>
        </button>

        <button 
          className={`nav-tab ${activeTab === 'students' ? 'active' : ''}`}
          onClick={() => setActiveTab('students')}
        >
          <Users size={16} />
          <span>Students ({students.length})</span>
        </button>

        <button 
          className={`nav-tab ${activeTab === 'rooms' ? 'active' : ''}`}
          onClick={() => setActiveTab('rooms')}
        >
          <Building2 size={16} />
          <span>Rooms ({rooms.length})</span>
        </button>

        <button 
          className={`nav-tab ${activeTab === 'exams' ? 'active' : ''}`}
          onClick={() => setActiveTab('exams')}
        >
          <Calendar size={16} />
          <span>Exam Schedule ({exams.length})</span>
        </button>

        <button 
          className={`nav-tab ${activeTab === 'invigilators' ? 'active' : ''}`}
          onClick={() => setActiveTab('invigilators')}
        >
          <ShieldCheck size={16} />
          <span>Invigilators ({invigilators.length})</span>
        </button>

        <button 
          className={`nav-tab ${activeTab === 'reports' ? 'active' : ''}`}
          onClick={() => setActiveTab('reports')}
        >
          <FileText size={16} />
          <span>Reports & CSV</span>
        </button>
      </nav>

      {/* Content Area */}
      <main className="main-content">

        {/* 1. DASHBOARD VIEW */}
        {activeTab === 'dashboard' && (
          <div className="fade-in">
            {/* Quick Metrics */}
            <div className="stats-grid">
              <div className="stat-card">
                <div className="stat-icon" style={{ background: '#e0f2fe', color: '#0284c7' }}>
                  <Users size={24} />
                </div>
                <div>
                  <div className="stat-value">{students.length}</div>
                  <div className="stat-label">Total Registered Students</div>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon" style={{ background: '#fef3c7', color: '#d97706' }}>
                  <Building2 size={24} />
                </div>
                <div>
                  <div className="stat-value">{rooms.length}</div>
                  <div className="stat-label">Exam Rooms Available</div>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon" style={{ background: '#d1fae5', color: '#059669' }}>
                  <Layers size={24} />
                </div>
                <div>
                  <div className="stat-value">{totalCapacity}</div>
                  <div className="stat-label">Total Seating Capacity</div>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon" style={{ background: '#f3e8ff', color: '#9333ea' }}>
                  <Calendar size={24} />
                </div>
                <div>
                  <div className="stat-value">{exams.length}</div>
                  <div className="stat-label">Scheduled Exams</div>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon" style={{ background: '#ffe4e6', color: '#e11d48' }}>
                  <ShieldCheck size={24} />
                </div>
                <div>
                  <div className="stat-value">{invigilators.length}</div>
                  <div className="stat-label">Available Invigilators</div>
                </div>
              </div>
            </div>

            {/* Quick Action Banner */}
            <div className="card" style={{ background: 'linear-gradient(135deg, #312e81, #4338ca)', color: 'white' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
                <div>
                  <h2 style={{ fontSize: '1.4rem', fontWeight: 700, marginBottom: '0.4rem' }}>
                    Exam Session: {selectedDate} ({selectedSlot})
                  </h2>
                  <p style={{ opacity: 0.9, fontSize: '0.88rem' }}>
                    Ready to generate a conflict-free seating matrix and assign invigilators.
                  </p>
                </div>
                <div style={{ display: 'flex', gap: '0.75rem' }}>
                  <button 
                    className="btn btn-primary" 
                    style={{ background: '#ffffff', color: '#4338ca', fontWeight: 600 }}
                    onClick={handleRunAllocation}
                  >
                    <Sparkles size={16} />
                    <span>Run Allocation Now</span>
                  </button>
                  <button 
                    className="btn btn-secondary" 
                    style={{ background: 'rgba(255,255,255,0.15)', color: 'white', borderColor: 'transparent' }}
                    onClick={() => setActiveTab('seating')}
                  >
                    <Layers size={16} />
                    <span>View Seating Chart</span>
                  </button>
                </div>
              </div>
            </div>

            {/* Subject Breakdown & Session Information */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.5rem' }}>
              <div className="card">
                <div className="card-header">
                  <div className="card-title">
                    <Calendar size={18} />
                    <span>Active Exam Subjects</span>
                  </div>
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                  {subjectsList.map(sub => {
                    const count = students.filter(s => s.subjectCode === sub).length;
                    const style = getSubjectBadgeStyle(sub);
                    return (
                      <div 
                        key={sub}
                        style={{
                          background: style.bg,
                          color: style.text,
                          border: `1px solid ${style.border}`,
                          padding: '0.5rem 0.85rem',
                          borderRadius: '8px',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.5rem',
                          fontWeight: 600,
                          fontSize: '0.85rem'
                        }}
                      >
                        <span>{sub}</span>
                        <span style={{ background: 'white', padding: '0.15rem 0.45rem', borderRadius: '4px', fontSize: '0.75rem' }}>
                          {count} students
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>

              <div className="card">
                <div className="card-header">
                  <div className="card-title">
                    <Building2 size={18} />
                    <span>Room Capacity Utilization</span>
                  </div>
                </div>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '0.5rem', fontWeight: 600 }}>
                    <span>Registered Students: {students.length}</span>
                    <span>Total Seats: {totalCapacity}</span>
                  </div>
                  <div style={{ width: '100%', height: '12px', background: '#e2e8f0', borderRadius: '6px', overflow: 'hidden' }}>
                    <div 
                      style={{ 
                        width: `${Math.min(100, (students.length / (totalCapacity || 1)) * 100)}%`, 
                        height: '100%', 
                        background: students.length > totalCapacity ? '#ef4444' : '#10b981' 
                      }} 
                    />
                  </div>
                  <p style={{ fontSize: '0.78rem', color: varCss('--text-muted'), marginTop: '0.5rem' }}>
                    {students.length > totalCapacity 
                      ? '⚠️ Warning: Insufficient seats. Add more rooms before running allocation.' 
                      : `✓ Capacity headroom: ${totalCapacity - students.length} vacant seats.`}
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* 2. ALLOCATION ENGINE VIEW */}
        {activeTab === 'allocation' && (
          <div className="fade-in">
            <div className="card">
              <div className="card-header">
                <div className="card-title">
                  <Sparkles size={20} />
                  <span>Execute Seating & Invigilation Allocation</span>
                </div>
              </div>

              {/* Form Controls */}
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'flex-end', marginBottom: '1.5rem' }}>
                <div>
                  <label className="form-label">Exam Date</label>
                  <input 
                    type="date" 
                    className="input" 
                    value={selectedDate}
                    onChange={(e) => setSelectedDate(e.target.value)}
                  />
                </div>

                <div>
                  <label className="form-label">Time Slot</label>
                  <select 
                    className="select"
                    value={selectedSlot}
                    onChange={(e) => setSelectedSlot(e.target.value)}
                  >
                    <option value="MORNING">MORNING</option>
                    <option value="AFTERNOON">AFTERNOON</option>
                    <option value="EVENING">EVENING</option>
                  </select>
                </div>

                <button 
                  className="btn btn-primary"
                  onClick={handleRunAllocation}
                  disabled={loading}
                >
                  <Sparkles size={16} />
                  <span>{loading ? 'Running Allocation...' : 'Run Allocation'}</span>
                </button>

                <button 
                  className="btn btn-danger"
                  onClick={handleClearAllocation}
                  disabled={loading}
                >
                  <Trash2 size={16} />
                  <span>Clear Session</span>
                </button>

                <button 
                  className="btn btn-secondary"
                  onClick={handleDownloadCsv}
                  disabled={!structuredAllocations}
                >
                  <Download size={16} />
                  <span>Export CSV</span>
                </button>
              </div>

              {/* Allocation Result Summary */}
              {allocationSummary && (
                <div className="fade-in" style={{ background: '#f8fafc', border: '1px solid #cbd5e1', borderRadius: '10px', padding: '1.25rem', marginTop: '1rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#047857', fontWeight: 700, marginBottom: '0.75rem' }}>
                    <CheckCircle2 size={20} />
                    <span>Allocation Computed & Saved Transactionally to MySQL</span>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', margin: '1rem 0' }}>
                    <div style={{ background: 'white', padding: '0.85rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                      <div style={{ fontSize: '0.75rem', color: '#64748b' }}>Students Seated</div>
                      <div style={{ fontSize: '1.4rem', fontWeight: 700, color: '#0f172a' }}>{allocationSummary.seated}</div>
                    </div>
                    <div style={{ background: 'white', padding: '0.85rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                      <div style={{ fontSize: '0.75rem', color: '#64748b' }}>Rooms Utilized</div>
                      <div style={{ fontSize: '1.4rem', fontWeight: 700, color: '#0f172a' }}>{allocationSummary.roomsUsed}</div>
                    </div>
                    <div style={{ background: 'white', padding: '0.85rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                      <div style={{ fontSize: '0.75rem', color: '#64748b' }}>Adjacency Warnings</div>
                      <div style={{ fontSize: '1.4rem', fontWeight: 700, color: allocationSummary.warnings.length > 0 ? '#d97706' : '#059669' }}>
                        {allocationSummary.warnings.length}
                      </div>
                    </div>
                  </div>

                  {/* Invigilators Assigned */}
                  <div style={{ marginTop: '1rem' }}>
                    <h4 style={{ fontSize: '0.88rem', fontWeight: 600, color: '#1e293b', marginBottom: '0.5rem' }}>
                      Room-wise Invigilator Assignments:
                    </h4>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.6rem' }}>
                      {allocationSummary.invigilators.map((inv, idx) => (
                        <div key={idx} className="badge badge-purple" style={{ padding: '0.45rem 0.75rem', fontSize: '0.82rem' }}>
                          <ShieldCheck size={14} />
                          <span>{inv.room}: {inv.invigilator}</span>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Warnings if any */}
                  {allocationSummary.warnings.length > 0 && (
                    <div style={{ marginTop: '1.25rem', background: '#fffbeb', border: '1px solid #fde68a', borderRadius: '8px', padding: '1rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#b45309', fontWeight: 600, marginBottom: '0.4rem' }}>
                        <AlertTriangle size={18} />
                        <span>Adjacency Warnings (Same-subject neighbors placed as fallback):</span>
                      </div>
                      <ul style={{ paddingLeft: '1.5rem', fontSize: '0.82rem', color: '#92400e' }}>
                        {allocationSummary.warnings.map((w, idx) => (
                          <li key={idx}>{w}</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        )}

        {/* 3. VISUAL SEATING CHART VIEW */}
        {activeTab === 'seating' && (
          <div className="fade-in">
            <div className="card">
              <div className="card-header">
                <div>
                  <div className="card-title">
                    <Layers size={20} />
                    <span>Room-Wise Interactive Seating Grid</span>
                  </div>
                  <div style={{ fontSize: '0.82rem', color: '#64748b', marginTop: '0.2rem' }}>
                    Session: {selectedDate} ({selectedSlot})
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
                  <div style={{ position: 'relative' }}>
                    <Search size={16} style={{ position: 'absolute', left: '10px', top: '10px', color: '#94a3b8' }} />
                    <input 
                      type="text" 
                      placeholder="Highlight student / roll..."
                      className="input"
                      style={{ paddingLeft: '32px', width: '220px' }}
                      value={seatSearch}
                      onChange={(e) => setSeatSearch(e.target.value)}
                    />
                  </div>
                  <button className="btn btn-secondary btn-sm" onClick={handleDownloadCsv}>
                    <Download size={14} />
                    <span>CSV</span>
                  </button>
                </div>
              </div>

              {/* Render Room Grids */}
              {structuredAllocations && structuredAllocations.rooms && structuredAllocations.rooms.length > 0 ? (
                <div className="room-seating-container">
                  {structuredAllocations.rooms.map((room) => {
                    // Build 2D grid matrix
                    const matrix = Array.from({ length: room.rows }, () => 
                      Array.from({ length: room.cols }, () => null)
                    );

                    room.seats.forEach(seat => {
                      if (seat.row - 1 < room.rows && seat.col - 1 < room.cols) {
                        matrix[seat.row - 1][seat.col - 1] = seat;
                      }
                    });

                    return (
                      <div key={room.roomId} className="room-board">
                        <div className="room-board-header">
                          <div>
                            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#1e293b' }}>
                              {room.roomName}
                            </h3>
                            <span style={{ fontSize: '0.78rem', color: '#64748b' }}>
                              Grid: {room.rows} rows × {room.cols} columns ({room.rows * room.cols} total seats)
                            </span>
                          </div>

                          <div className="badge badge-blue" style={{ fontSize: '0.85rem', padding: '0.5rem 0.85rem' }}>
                            <ShieldCheck size={16} />
                            <span>Invigilator: {room.invigilator}</span>
                          </div>
                        </div>

                        {/* Teacher's Desk / Front of Room */}
                        <div className="teacher-desk">
                          <MapPin size={14} />
                          <span>FRONT OF ROOM / TEACHER'S DESK</span>
                        </div>

                        {/* Student Desks Grid */}
                        <div 
                          className="desks-grid"
                          style={{
                            gridTemplateColumns: `repeat(${room.cols}, minmax(130px, 1fr))`
                          }}
                        >
                          {matrix.map((rowArr, rIdx) => 
                            rowArr.map((seat, cIdx) => {
                              if (!seat) {
                                return (
                                  <div key={`${rIdx}-${cIdx}`} className="desk-seat empty">
                                    <div className="desk-coordinate">R{rIdx + 1}C{cIdx + 1}</div>
                                    <span style={{ fontSize: '0.75rem' }}>Empty Seat</span>
                                  </div>
                                );
                              }

                              const style = getSubjectBadgeStyle(seat.subjectCode);
                              const isMatch = seatSearch && (
                                seat.name.toLowerCase().includes(seatSearch.toLowerCase()) ||
                                String(seat.rollNo).includes(seatSearch)
                              );

                              return (
                                <div 
                                  key={`${rIdx}-${cIdx}`} 
                                  className="desk-seat"
                                  style={{
                                    borderLeft: `4px solid ${style.text}`,
                                    background: isMatch ? '#fef08a' : 'white',
                                    boxShadow: isMatch ? '0 0 0 2px #eab308' : undefined
                                  }}
                                >
                                  <div>
                                    <div className="desk-coordinate">R{seat.row}C{seat.col}</div>
                                    <div className="desk-student-name" title={seat.name}>
                                      {seat.name}
                                    </div>
                                    <div className="desk-student-roll">
                                      Roll: #{seat.rollNo}
                                    </div>
                                  </div>
                                  <div 
                                    className="badge desk-subject-badge"
                                    style={{ background: style.bg, color: style.text, border: `1px solid ${style.border}` }}
                                  >
                                    {seat.subjectCode}
                                  </div>
                                </div>
                              );
                            })
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div style={{ textAlign: 'center', padding: '3.5rem 1rem', color: '#64748b' }}>
                  <Layers size={48} style={{ opacity: 0.3, margin: '0 auto 1rem auto' }} />
                  <p style={{ fontWeight: 600, fontSize: '1rem', color: '#1e293b' }}>
                    No Seating Allocation Saved for {selectedDate} ({selectedSlot})
                  </p>
                  <p style={{ fontSize: '0.85rem', marginTop: '0.25rem', marginBottom: '1.25rem' }}>
                    Switch to the Allocation Engine tab and click "Run Allocation" to generate the seating chart.
                  </p>
                  <button className="btn btn-primary" onClick={() => setActiveTab('allocation')}>
                    <Sparkles size={16} />
                    <span>Go to Allocation Engine</span>
                  </button>
                </div>
              )}
            </div>
          </div>
        )}

        {/* 4. STUDENTS VIEW */}
        {activeTab === 'students' && (
          <div className="fade-in">
            <div className="card">
              <div className="card-header">
                <div className="card-title">
                  <Users size={20} />
                  <span>Student Directory</span>
                </div>

                <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                  {/* Search */}
                  <div style={{ position: 'relative' }}>
                    <Search size={16} style={{ position: 'absolute', left: '10px', top: '10px', color: '#94a3b8' }} />
                    <input 
                      type="text" 
                      placeholder="Search name or roll..."
                      className="input"
                      style={{ paddingLeft: '32px' }}
                      value={studentSearch}
                      onChange={(e) => setStudentSearch(e.target.value)}
                    />
                  </div>

                  {/* Filter Subject */}
                  <select 
                    className="select"
                    value={selectedSubjectFilter}
                    onChange={(e) => setSelectedSubjectFilter(e.target.value)}
                  >
                    <option value="ALL">All Subjects</option>
                    {subjectsList.map(s => <option key={s} value={s}>{s}</option>)}
                  </select>

                  <button className="btn btn-primary" onClick={() => setModalType('student')}>
                    <Plus size={16} />
                    <span>Add Student</span>
                  </button>
                </div>
              </div>

              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Roll Number</th>
                      <th>Student Name</th>
                      <th>Registered Subject</th>
                      <th style={{ textAlign: 'right' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredStudents.length > 0 ? (
                      filteredStudents.map(s => {
                        const style = getSubjectBadgeStyle(s.subjectCode);
                        return (
                          <tr key={s.rollNo}>
                            <td style={{ fontWeight: 600 }}>#{s.rollNo}</td>
                            <td>{s.name}</td>
                            <td>
                              <span 
                                className="badge"
                                style={{ background: style.bg, color: style.text, border: `1px solid ${style.border}` }}
                              >
                                {s.subjectCode}
                              </span>
                            </td>
                            <td style={{ textAlign: 'right' }}>
                              <button 
                                className="btn btn-danger btn-sm"
                                onClick={() => handleDeleteStudent(s.rollNo)}
                              >
                                <Trash2 size={13} />
                                <span>Delete</span>
                              </button>
                            </td>
                          </tr>
                        );
                      })
                    ) : (
                      <tr>
                        <td colSpan="4" style={{ textAlign: 'center', padding: '2rem', color: '#94a3b8' }}>
                          No students matching criteria
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* 5. ROOMS VIEW */}
        {activeTab === 'rooms' && (
          <div className="fade-in">
            <div className="card">
              <div className="card-header">
                <div className="card-title">
                  <Building2 size={20} />
                  <span>Exam Rooms & Venues</span>
                </div>

                <button className="btn btn-primary" onClick={() => setModalType('room')}>
                  <Plus size={16} />
                  <span>Add Room</span>
                </button>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1.25rem' }}>
                {rooms.map(room => (
                  <div key={room.roomId} className="card" style={{ marginBottom: 0 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
                      <div>
                        <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#1e293b' }}>{room.roomName}</h4>
                        <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Room ID: #{room.roomId}</span>
                      </div>
                      <button 
                        className="btn btn-danger btn-sm"
                        onClick={() => handleDeleteRoom(room.roomId, room.roomName)}
                      >
                        <Trash2 size={13} />
                      </button>
                    </div>

                    <div style={{ display: 'flex', gap: '0.5rem', margin: '0.85rem 0' }}>
                      <div className="badge badge-blue">
                        <span>{room.rows} Rows</span>
                      </div>
                      <div className="badge badge-purple">
                        <span>{room.cols} Columns</span>
                      </div>
                      <div className="badge badge-emerald">
                        <span>{room.capacity} Total Seats</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* 6. EXAMS VIEW */}
        {activeTab === 'exams' && (
          <div className="fade-in">
            <div className="card">
              <div className="card-header">
                <div className="card-title">
                  <Calendar size={20} />
                  <span>Exam Schedule</span>
                </div>

                <button className="btn btn-primary" onClick={() => setModalType('exam')}>
                  <Plus size={16} />
                  <span>Schedule Exam</span>
                </button>
              </div>

              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Exam ID</th>
                      <th>Subject Code</th>
                      <th>Exam Date</th>
                      <th>Slot</th>
                      <th style={{ textAlign: 'right' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {exams.map(e => {
                      const style = getSubjectBadgeStyle(e.subjectCode);
                      return (
                        <tr key={e.examId}>
                          <td style={{ fontWeight: 600 }}>#{e.examId}</td>
                          <td>
                            <span 
                              className="badge"
                              style={{ background: style.bg, color: style.text, border: `1px solid ${style.border}` }}
                            >
                              {e.subjectCode}
                            </span>
                          </td>
                          <td>{e.examDate}</td>
                          <td>
                            <span className="badge badge-purple">
                              <Clock size={12} />
                              <span>{e.slot}</span>
                            </span>
                          </td>
                          <td style={{ textAlign: 'right' }}>
                            <button 
                              className="btn btn-danger btn-sm"
                              onClick={() => handleDeleteExam(e.examId)}
                            >
                              <Trash2 size={13} />
                              <span>Delete</span>
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* 7. INVIGILATORS VIEW */}
        {activeTab === 'invigilators' && (
          <div className="fade-in">
            <div className="card">
              <div className="card-header">
                <div className="card-title">
                  <ShieldCheck size={20} />
                  <span>Invigilator Faculty Pool</span>
                </div>

                <button className="btn btn-primary" onClick={() => setModalType('invigilator')}>
                  <Plus size={16} />
                  <span>Add Invigilator</span>
                </button>
              </div>

              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Invigilator ID</th>
                      <th>Full Name / Designation</th>
                      <th style={{ textAlign: 'right' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {invigilators.map(inv => (
                      <tr key={inv.invigilatorId}>
                        <td style={{ fontWeight: 600 }}>#{inv.invigilatorId}</td>
                        <td style={{ fontWeight: 500 }}>{inv.name}</td>
                        <td style={{ textAlign: 'right' }}>
                          <button 
                            className="btn btn-danger btn-sm"
                            onClick={() => handleDeleteInvigilator(inv.invigilatorId, inv.name)}
                          >
                            <Trash2 size={13} />
                            <span>Delete</span>
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* 8. REPORTS & CSV VIEW */}
        {activeTab === 'reports' && (
          <div className="fade-in">
            <div className="card">
              <div className="card-header">
                <div>
                  <div className="card-title">
                    <FileText size={20} />
                    <span>Official Seating Chart Report</span>
                  </div>
                  <div style={{ fontSize: '0.82rem', color: '#64748b', marginTop: '0.2rem' }}>
                    Formatted room-wise matrix for display on notice boards and exam doors.
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '0.75rem' }}>
                  <button className="btn btn-primary" onClick={handleDownloadCsv}>
                    <Download size={16} />
                    <span>Download CSV (Excel)</span>
                  </button>
                </div>
              </div>

              {chartText ? (
                <pre className="chart-pre">{chartText}</pre>
              ) : (
                <div style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>
                  <FileText size={42} style={{ opacity: 0.3, margin: '0 auto 1rem auto' }} />
                  <p>No report available for the current date and slot.</p>
                </div>
              )}
            </div>
          </div>
        )}

      </main>

      {/* --- MODAL DIALOGS --- */}

      {/* Add Student Modal */}
      {modalType === 'student' && (
        <div className="modal-backdrop">
          <div className="modal-content">
            <div className="modal-header">
              <div className="modal-title">Register New Student</div>
              <button className="btn btn-secondary btn-sm" onClick={() => setModalType(null)}>
                <X size={16} />
              </button>
            </div>
            <form onSubmit={handleAddStudent}>
              <div className="form-group">
                <label className="form-label">Roll Number</label>
                <input 
                  type="number" 
                  required
                  className="input" 
                  style={{ width: '100%' }}
                  placeholder="e.g. 113"
                  value={studentForm.rollNo}
                  onChange={(e) => setStudentForm({ ...studentForm, rollNo: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Full Name</label>
                <input 
                  type="text" 
                  required
                  className="input" 
                  style={{ width: '100%' }}
                  placeholder="e.g. Ananya Sen"
                  value={studentForm.name}
                  onChange={(e) => setStudentForm({ ...studentForm, name: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Subject Code</label>
                <input 
                  type="text" 
                  required
                  className="input" 
                  style={{ width: '100%', textTransform: 'uppercase' }}
                  placeholder="e.g. DBMS, JAVA, CN"
                  value={studentForm.subjectCode}
                  onChange={(e) => setStudentForm({ ...studentForm, subjectCode: e.target.value })}
                />
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setModalType(null)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save Student</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Room Modal */}
      {modalType === 'room' && (
        <div className="modal-backdrop">
          <div className="modal-content">
            <div className="modal-header">
              <div className="modal-title">Create Examination Room</div>
              <button className="btn btn-secondary btn-sm" onClick={() => setModalType(null)}>
                <X size={16} />
              </button>
            </div>
            <form onSubmit={handleAddRoom}>
              <div className="form-group">
                <label className="form-label">Room Name / Number</label>
                <input 
                  type="text" 
                  required
                  className="input" 
                  style={{ width: '100%' }}
                  placeholder="e.g. Room D or Hall 101"
                  value={roomForm.roomName}
                  onChange={(e) => setRoomForm({ ...roomForm, roomName: e.target.value })}
                />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Rows (1-30)</label>
                  <input 
                    type="number" 
                    min="1" 
                    max="30"
                    required
                    className="input" 
                    style={{ width: '100%' }}
                    placeholder="e.g. 5"
                    value={roomForm.rows}
                    onChange={(e) => setRoomForm({ ...roomForm, rows: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Columns (1-30)</label>
                  <input 
                    type="number" 
                    min="1" 
                    max="30"
                    required
                    className="input" 
                    style={{ width: '100%' }}
                    placeholder="e.g. 6"
                    value={roomForm.cols}
                    onChange={(e) => setRoomForm({ ...roomForm, cols: e.target.value })}
                  />
                </div>
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setModalType(null)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Create Room</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Exam Modal */}
      {modalType === 'exam' && (
        <div className="modal-backdrop">
          <div className="modal-content">
            <div className="modal-header">
              <div className="modal-title">Schedule New Exam</div>
              <button className="btn btn-secondary btn-sm" onClick={() => setModalType(null)}>
                <X size={16} />
              </button>
            </div>
            <form onSubmit={handleAddExam}>
              <div className="form-group">
                <label className="form-label">Subject Code</label>
                <input 
                  type="text" 
                  required
                  className="input" 
                  style={{ width: '100%', textTransform: 'uppercase' }}
                  placeholder="e.g. OS, DSA"
                  value={examForm.subjectCode}
                  onChange={(e) => setExamForm({ ...examForm, subjectCode: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Exam Date (yyyy-MM-dd)</label>
                <input 
                  type="date" 
                  required
                  className="input" 
                  style={{ width: '100%' }}
                  value={examForm.examDate}
                  onChange={(e) => setExamForm({ ...examForm, examDate: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Slot</label>
                <select 
                  className="select" 
                  style={{ width: '100%' }}
                  value={examForm.slot}
                  onChange={(e) => setExamForm({ ...examForm, slot: e.target.value })}
                >
                  <option value="MORNING">MORNING</option>
                  <option value="AFTERNOON">AFTERNOON</option>
                  <option value="EVENING">EVENING</option>
                </select>
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setModalType(null)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Schedule Exam</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Invigilator Modal */}
      {modalType === 'invigilator' && (
        <div className="modal-backdrop">
          <div className="modal-content">
            <div className="modal-header">
              <div className="modal-title">Add Invigilator to Roster</div>
              <button className="btn btn-secondary btn-sm" onClick={() => setModalType(null)}>
                <X size={16} />
              </button>
            </div>
            <form onSubmit={handleAddInvigilator}>
              <div className="form-group">
                <label className="form-label">Faculty Name & Title</label>
                <input 
                  type="text" 
                  required
                  className="input" 
                  style={{ width: '100%' }}
                  placeholder="e.g. Dr. Rajesh Kumar"
                  value={invigilatorForm.name}
                  onChange={(e) => setInvigilatorForm({ ...invigilatorForm, name: e.target.value })}
                />
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setModalType(null)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Add Invigilator</button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}

function varCss(v) {
  return `var(${v})`;
}
