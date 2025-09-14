import { useEffect, useState } from 'react'
import './index.css'
import MonsterCard from './components/MonsterCard'

function App() {

  const [socket, setSocket] = useState(null);

  const [playerMonster, setPlayerMonster] = useState({
    name: 'Goblin',
    hp: 10,
    maxHp: 10,
    actions: [
      { name: 'Scratch', damage: 2 },
      { name: 'Bite', damage: 3 }
    ]
  })

  const [opponentMonster, setOpponentMonster] = useState({
    name: 'Orc',
    hp: 12,
    maxHp: 12
  })

  const [playerTurn, setPlayerTurn] = useState(true);

  useEffect(() => {
    const newSocket = new WebSocket('ws://localhost:8080');

    newSocket.onopen = () => {
      console.log('Connected to WebSocket server');
      setSocket(newSocket);
    };

    newSocket.onmessage = (event) => {
      const message = JSON.parse(event.data);
      console.log("Message: " + message)
      console.log(event.data)
    }

    return () => {
      newSocket.close();
    }

  }, [])


  useEffect(() => {
    if (!playerTurn) {
      const timer = setTimeout(() => {
        const action = { name: 'Smash', damage: 2 };
        const newHp = Math.max(playerMonster.hp - action.damage, 0);
        setPlayerMonster({ ...playerMonster, hp: newHp });
        setPlayerTurn(true);
      }, 1000);
      return () => clearTimeout(timer);
    }
  }, [playerTurn])

  const handlePlayerAction = (action) => {
    if (socket && playerTurn) {
      const message = {
        type: 'PLAYER_ACTION',
        payload: action
      }
      const jsonmessage = JSON.stringify(message);
      socket.send(jsonmessage);
    }
  }

  const handleSendMessage = () => {
    if (socket) {
      socket.send('Hello from client!');
    }
  }

  return (
    <>
      <div className="p-4">
        <button onClick={handleSendMessage} className="bg-green-500 px-4 py-2 rounded">
          Send Test Message
        </button>
      </div>
      <main className="bg-slate-800 text-white min-h-screen flex flex-col">
        {/* Zona del Oponente */}
        <div className='h-1/2 flex flex-col items-center justify-center'>
          <MonsterCard monster={opponentMonster} />
        </div>

        {/* Zona del Jugador */}
        <div className='h-1/2 flex flex-col items-center justify-center'>
          <MonsterCard monster={playerMonster} />
          <div className="mt-4">
            {playerMonster.actions.map((action, index) => (
              <button key={index} onClick={() => handlePlayerAction(action)} className={`bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded m-2 ${!playerTurn ? 'opacity-50 cursor-not-allowed' : ''}`} disabled={!playerTurn}>
                {action.name} ({action.damage} DMG)
              </button>
            ))}
          </div>
        </div>
      </main>
    </>
  )
}

export default App
